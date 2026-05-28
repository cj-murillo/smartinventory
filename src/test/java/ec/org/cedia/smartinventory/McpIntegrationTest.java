package ec.org.cedia.smartinventory;

import java.util.List;
import java.util.Map;

import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.mcp.McpToolService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class McpIntegrationTest {

    @Autowired
    private HttpServletStreamableServerTransportProvider transportProvider;

    @Autowired
    private McpSyncServer mcpServer;

    @Autowired
    private McpToolService mcpToolService;

    @Test
    void mcpSdk_debeRegistrarTransportYTools() {
        assertNotNull(transportProvider);
        assertNotNull(mcpServer);
        assertTrue(transportProvider.protocolVersions().contains("2025-11-25"));
        assertTrue(transportProvider.protocolVersions().contains("2025-06-18"));

        List<String> toolNames = mcpServer.listTools().stream()
                .map(McpSchema.Tool::name)
                .toList();

        assertTrue(toolNames.contains("list_products"));
        assertTrue(toolNames.contains("get_product"));
        assertTrue(toolNames.contains("create_product"));
        assertTrue(toolNames.contains("update_product"));
        assertTrue(toolNames.contains("delete_product"));
        assertTrue(toolNames.contains("health_check"));
    }

    @Test
    void createProduct_desdeToolSdk_debeFuncionar() {
        McpServerFeatures.SyncToolSpecification tool = findTool("create_product");

        McpSchema.CallToolResult result = tool.callHandler().apply(null, new McpSchema.CallToolRequest(
                "create_product",
                Map.of(
                        "name", "Mouse SDK",
                        "description", "Mouse de prueba",
                        "price", 25.5,
                        "stock", 7
                )
        ));

        assertFalse(Boolean.TRUE.equals(result.isError()));
        Map<?, ?> structured = (Map<?, ?>) result.structuredContent();
        ProductResponseDTO product = (ProductResponseDTO) structured.get("product");
        assertEquals("Mouse SDK", product.getName());
        assertNotNull(product.getId());
    }

    @Test
    void createProduct_invalido_desdeToolSdk_debeRetornarError() {
        McpServerFeatures.SyncToolSpecification tool = findTool("create_product");

        McpSchema.CallToolResult result = tool.callHandler().apply(null, new McpSchema.CallToolRequest(
                "create_product",
                Map.of(
                        "price", 10.0,
                        "stock", 2
                )
        ));

        assertTrue(Boolean.TRUE.equals(result.isError()));
        Map<?, ?> structured = (Map<?, ?>) result.structuredContent();
        Map<?, ?> fields = (Map<?, ?>) structured.get("fields");
        assertEquals("INVALID_ARGUMENT", structured.get("code"));
        assertTrue(fields.containsKey("name"));
    }

    @Test
    void healthCheck_desdeToolSdk_debeRetornarUp() {
        McpServerFeatures.SyncToolSpecification tool = findTool("health_check");

        McpSchema.CallToolResult result = tool.callHandler().apply(null, new McpSchema.CallToolRequest(
                "health_check",
                Map.of()
        ));

        assertFalse(Boolean.TRUE.equals(result.isError()));
        Map<?, ?> structured = (Map<?, ?>) result.structuredContent();
        assertEquals("UP", structured.get("status"));
        assertEquals("SmartInventory MCP", structured.get("service"));
    }

    private McpServerFeatures.SyncToolSpecification findTool(String name) {
        return mcpToolService.toolSpecifications().stream()
                .filter(spec -> spec.tool().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tool no encontrada: " + name));
    }
}
