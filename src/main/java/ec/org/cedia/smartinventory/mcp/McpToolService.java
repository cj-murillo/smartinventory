package ec.org.cedia.smartinventory.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.exception.ResourceNotFoundException;
import ec.org.cedia.smartinventory.mcp.dto.DeleteProductToolInput;
import ec.org.cedia.smartinventory.mcp.dto.GetProductToolInput;
import ec.org.cedia.smartinventory.mcp.dto.ListProductsToolInput;
import ec.org.cedia.smartinventory.service.ProductUseCase;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class McpToolService {

    private final ProductUseCase productUseCase;
    private final Validator validator;
    private final McpJsonMapper mcpJsonMapper;

    public List<McpServerFeatures.SyncToolSpecification> toolSpecifications() {
        return List.of(
                listProductsTool(),
                getProductTool(),
                createProductTool(),
                updateProductTool(),
                deleteProductTool(),
                healthCheckTool()
        );
    }

    private McpServerFeatures.SyncToolSpecification listProductsTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("list_products")
                        .title("list_products")
                        .description("Lista productos paginados")
                        .inputSchema(jsonSchema(
                                Map.of(
                                        "page", Map.of("type", "integer", "minimum", 0),
                                        "size", Map.of("type", "integer", "minimum", 1, "maximum", 100)
                                ),
                                List.of()))
                        .build())
                .callHandler((exchange, request) -> {
                    ListProductsToolInput input = mcpJsonMapper.convertValue(request.arguments(), ListProductsToolInput.class);
                    Optional<McpSchema.CallToolResult> validationError = validate(input);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    int page = input.page() == null ? 0 : input.page();
                    int size = input.size() == null ? 10 : input.size();
                    Page<ProductResponseDTO> result = productUseCase.listarProductos(PageRequest.of(page, size));

                    return success("Productos listados correctamente", Map.of(
                            "items", result.getContent(),
                            "page", result.getNumber(),
                            "size", result.getSize(),
                            "totalElements", result.getTotalElements(),
                            "totalPages", result.getTotalPages()
                    ));
                })
                .build();
    }

    private McpServerFeatures.SyncToolSpecification getProductTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("get_product")
                        .title("get_product")
                        .description("Obtiene un producto por id")
                        .inputSchema(jsonSchema(
                                Map.of("id", Map.of("type", "integer", "minimum", 1)),
                                List.of("id")))
                        .build())
                .callHandler((exchange, request) -> {
                    GetProductToolInput input = mcpJsonMapper.convertValue(request.arguments(), GetProductToolInput.class);
                    Optional<McpSchema.CallToolResult> validationError = validate(input);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    try {
                        ProductResponseDTO product = productUseCase.obtenerProducto(input.id());
                        return success("Producto obtenido correctamente", Map.of("product", product));
                    } catch (ResourceNotFoundException ex) {
                        return domainError("NOT_FOUND", ex.getMessage());
                    }
                })
                .build();
    }

    private McpServerFeatures.SyncToolSpecification createProductTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("create_product")
                        .title("create_product")
                        .description("Crea un producto")
                        .inputSchema(productRequestSchema())
                        .build())
                .callHandler((exchange, request) -> {
                    ProductRequestDTO input = mcpJsonMapper.convertValue(request.arguments(), ProductRequestDTO.class);
                    Optional<McpSchema.CallToolResult> validationError = validate(input);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    ProductResponseDTO product = productUseCase.crearProducto(input);
                    return success("Producto creado correctamente", Map.of("product", product));
                })
                .build();
    }

    private McpServerFeatures.SyncToolSpecification updateProductTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("update_product")
                        .title("update_product")
                        .description("Actualiza un producto")
                        .inputSchema(jsonSchema(
                                new LinkedHashMap<>(Map.of(
                                        "id", Map.of("type", "integer", "minimum", 1),
                                        "name", Map.of("type", "string", "minLength", 2, "maxLength", 100),
                                        "description", Map.of("type", "string", "maxLength", 500),
                                        "price", Map.of("type", "number", "exclusiveMinimum", 0),
                                        "stock", Map.of("type", "integer", "minimum", 0)
                                )),
                                List.of("id", "name", "price", "stock")))
                        .build())
                .callHandler((exchange, request) -> {
                    Long id = numberToLong(request.arguments().get("id"));
                    if (id == null) {
                        return validationError(Map.of("id", "El id es obligatorio"));
                    }

                    ProductRequestDTO input = mcpJsonMapper.convertValue(request.arguments(), ProductRequestDTO.class);
                    Optional<McpSchema.CallToolResult> validationError = validate(input);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    try {
                        ProductResponseDTO product = productUseCase.actualizarProducto(id, input);
                        return success("Producto actualizado correctamente", Map.of("product", product));
                    } catch (ResourceNotFoundException ex) {
                        return domainError("NOT_FOUND", ex.getMessage());
                    }
                })
                .build();
    }

    private McpServerFeatures.SyncToolSpecification deleteProductTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("delete_product")
                        .title("delete_product")
                        .description("Elimina un producto por id")
                        .inputSchema(jsonSchema(
                                Map.of("id", Map.of("type", "integer", "minimum", 1)),
                                List.of("id")))
                        .build())
                .callHandler((exchange, request) -> {
                    DeleteProductToolInput input = mcpJsonMapper.convertValue(request.arguments(), DeleteProductToolInput.class);
                    Optional<McpSchema.CallToolResult> validationError = validate(input);
                    if (validationError.isPresent()) {
                        return validationError.get();
                    }

                    try {
                        productUseCase.eliminarProducto(input.id());
                        return success("Producto eliminado correctamente", Map.of(
                                "deleted", true,
                                "id", input.id()
                        ));
                    } catch (ResourceNotFoundException ex) {
                        return domainError("NOT_FOUND", ex.getMessage());
                    }
                })
                .build();
    }

    private McpServerFeatures.SyncToolSpecification healthCheckTool() {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name("health_check")
                        .title("health_check")
                        .description("Devuelve el estado del servicio")
                        .inputSchema(jsonSchema(Map.of(), List.of()))
                        .build())
                .callHandler((exchange, request) -> success("Servicio activo", Map.of(
                        "status", "UP",
                        "service", "SmartInventory MCP"
                )))
                .build();
    }

    private McpSchema.JsonSchema productRequestSchema() {
        return jsonSchema(
                new LinkedHashMap<>(Map.of(
                        "name", Map.of("type", "string", "minLength", 2, "maxLength", 100),
                        "description", Map.of("type", "string", "maxLength", 500),
                        "price", Map.of("type", "number", "exclusiveMinimum", 0),
                        "stock", Map.of("type", "integer", "minimum", 0)
                )),
                List.of("name", "price", "stock"));
    }

    private McpSchema.JsonSchema jsonSchema(Map<String, Object> properties, List<String> required) {
        return new McpSchema.JsonSchema("object", properties, required, false, null, null);
    }

    private <T> Optional<McpSchema.CallToolResult> validate(T target) {
        Set<ConstraintViolation<T>> violations = validator.validate(target);
        if (violations.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (ConstraintViolation<T> violation : violations) {
            fields.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return Optional.of(validationError(fields));
    }

    private McpSchema.CallToolResult success(String message, Map<String, Object> structuredContent) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .structuredContent(structuredContent)
                .isError(false)
                .build();
    }

    private McpSchema.CallToolResult validationError(Map<String, String> fields) {
        return McpSchema.CallToolResult.builder()
                .addTextContent("Parámetros inválidos")
                .structuredContent(Map.of(
                        "code", "INVALID_ARGUMENT",
                        "fields", fields
                ))
                .isError(true)
                .build();
    }

    private McpSchema.CallToolResult domainError(String code, String message) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .structuredContent(Map.of(
                        "code", code,
                        "message", message
                ))
                .isError(true)
                .build();
    }

    private Long numberToLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
