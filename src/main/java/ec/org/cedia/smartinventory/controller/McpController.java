package ec.org.cedia.smartinventory.controller;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ec.org.cedia.smartinventory.mcp.McpErrorCode;
import ec.org.cedia.smartinventory.mcp.McpProtocolException;
import ec.org.cedia.smartinventory.mcp.McpSession;
import ec.org.cedia.smartinventory.mcp.McpSessionRegistry;
import ec.org.cedia.smartinventory.mcp.McpToolResponse;
import ec.org.cedia.smartinventory.mcp.McpToolService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private static final String LATEST_PROTOCOL_VERSION = "2025-11-25";
    private static final String FALLBACK_PROTOCOL_VERSION = "2025-06-18";
    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS = Set.of(
            LATEST_PROTOCOL_VERSION,
            FALLBACK_PROTOCOL_VERSION
    );
    private static final String SESSION_HEADER = "Mcp-Session-Id";
    private static final String PROTOCOL_HEADER = "MCP-Protocol-Version";

    private final ObjectMapper objectMapper;
    private final McpSessionRegistry sessionRegistry;
    private final McpToolService mcpToolService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("UseSpecificCatch")
    public ResponseEntity<?> handlePost(
            @RequestBody String body,
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            @RequestHeader(value = PROTOCOL_HEADER, required = false) String protocolHeader,
            HttpServletRequest request) {

        validateOrigin(request);

        try {
            JsonNode payload = objectMapper.readTree(body);
            String method = textField(payload, "method");
            JsonNode id = payload.get("id");
            JsonNode params = payload.path("params");

            if ("initialize".equals(method)) {
                String requestedVersion = params.path("protocolVersion").asText(LATEST_PROTOCOL_VERSION);
                String negotiatedVersion = negotiateProtocolVersion(requestedVersion);
                McpSession session = sessionRegistry.create(negotiatedVersion);
                return ResponseEntity.ok()
                        .header(SESSION_HEADER, session.id())
                        .header(PROTOCOL_HEADER, session.protocolVersion())
                        .body(success(id, mcpToolService.initializeResult(session.protocolVersion())));
            }

            requireValidProtocol(protocolHeader);
            McpSession session = sessionRegistry.getRequired(sessionId);

            if (payload.has("id")) {
                return handleRequest(method, id, params, session);
            }

            if ("notifications/initialized".equals(method)) {
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .header(PROTOCOL_HEADER, session.protocolVersion())
                        .build();
            }

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .header(PROTOCOL_HEADER, session.protocolVersion())
                    .build();
        } catch (McpProtocolException ex) {
            return protocolError(extractId(body), ex);
        } catch (Exception ex) {
            return protocolError(extractId(body),
                    new McpProtocolException(McpErrorCode.INTERNAL_ERROR, ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Void> handleGet(HttpServletRequest request) {
        validateOrigin(request);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> handleDelete(
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionId,
            HttpServletRequest request) {
        validateOrigin(request);
        if (sessionId != null && !sessionId.isBlank()) {
            sessionRegistry.delete(sessionId);
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> handleRequest(String method, JsonNode id, JsonNode params, McpSession session) {
        return switch (method) {
            case "tools/list" -> ResponseEntity.ok()
                    .header(PROTOCOL_HEADER, session.protocolVersion())
                    .body(success(id, mcpToolService.listTools()));
            case "tools/call" -> {
                String toolName = params.path("name").asText(null);
                if (toolName == null || toolName.isBlank()) {
                    throw new McpProtocolException(McpErrorCode.INVALID_PARAMS, "El nombre de la tool es obligatorio");
                }
                Map<String, Object> arguments = params.has("arguments")
                        ? objectMapper.convertValue(params.get("arguments"), Map.class)
                        : Map.of();
                McpToolResponse result = mcpToolService.callTool(toolName, arguments);
                Map<String, Object> resultBody = new LinkedHashMap<>();
                resultBody.put("content", result.content());
                resultBody.put("structuredContent", result.structuredContent());
                resultBody.put("isError", result.isError());
                yield ResponseEntity.ok()
                        .header(PROTOCOL_HEADER, session.protocolVersion())
                        .body(success(id, resultBody));
            }
            default -> throw new McpProtocolException(McpErrorCode.METHOD_NOT_FOUND, "Método no soportado: " + method);
        };
    }

    private ResponseEntity<Map<String, Object>> protocolError(JsonNode id, McpProtocolException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", ex.getCode());
        error.put("message", ex.getMessage());
        if (ex.getData() != null) {
            error.put("data", ex.getData());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", id == null || id.isMissingNode() ? null : objectMapper.convertValue(id, Object.class));
        body.put("error", error);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> success(JsonNode id, Object result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", id == null || id.isMissingNode() ? null : objectMapper.convertValue(id, Object.class));
        body.put("result", result);
        return body;
    }

    private String textField(JsonNode node, String fieldName) {
        if (node == null || node.path(fieldName).isMissingNode()) {
            throw new McpProtocolException(McpErrorCode.INVALID_REQUEST, "Falta el campo " + fieldName);
        }
        return node.path(fieldName).asText();
    }

    private void requireValidProtocol(String protocolHeader) {
        if (protocolHeader == null || protocolHeader.isBlank()) {
            return;
        }
        if (!SUPPORTED_PROTOCOL_VERSIONS.contains(protocolHeader)) {
            throw new McpProtocolException(McpErrorCode.INVALID_REQUEST,
                    "Versión MCP no soportada: " + protocolHeader);
        }
    }

    private String negotiateProtocolVersion(String requestedVersion) {
        if (SUPPORTED_PROTOCOL_VERSIONS.contains(requestedVersion)) {
            return requestedVersion;
        }

        throw new McpProtocolException(
                McpErrorCode.INVALID_REQUEST,
                "Versión MCP no soportada: " + requestedVersion,
                Map.of("supported", supportedVersions())
        );
    }

    private Set<String> supportedVersions() {
        Set<String> versions = new LinkedHashSet<>();
        versions.add(LATEST_PROTOCOL_VERSION);
        versions.add(FALLBACK_PROTOCOL_VERSION);
        return versions;
    }

    private void validateOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) {
            return;
        }

        String host = request.getHeader(HttpHeaders.HOST);
        if (host != null && origin.contains(host)) {
            return;
        }

        throw new McpProtocolException(McpErrorCode.INVALID_REQUEST, "Origin no permitido");
    }

    private JsonNode extractId(String body) {
        try {
            return objectMapper.readTree(body).get("id");
        } catch (Exception ex) {
            return null;
        }
    }
}
