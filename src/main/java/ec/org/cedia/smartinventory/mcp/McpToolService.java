package ec.org.cedia.smartinventory.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.exception.ResourceNotFoundException;
import ec.org.cedia.smartinventory.mcp.dto.DeleteProductToolInput;
import ec.org.cedia.smartinventory.mcp.dto.GetProductToolInput;
import ec.org.cedia.smartinventory.mcp.dto.ListProductsToolInput;
import ec.org.cedia.smartinventory.service.ProductUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class McpToolService {

    private final ProductUseCase productUseCase;
    private final McpValidationUtils validationUtils;
    private final ObjectMapper objectMapper;

    public Map<String, Object> initializeResult(String protocolVersion) {
        return Map.of(
                "protocolVersion", protocolVersion,
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", false)
                ),
                "serverInfo", Map.of(
                        "name", "smartinventory-mcp",
                        "version", "0.1.0"
                ),
                "instructions", "Usa las tools de productos para consultar y modificar el inventario.");
    }

    public Map<String, Object> listTools() {
        return Map.of("tools", List.of(
                tool("list_products", "Lista productos paginados", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "page", Map.of("type", "integer", "minimum", 0),
                                "size", Map.of("type", "integer", "minimum", 1, "maximum", 100)
                        )
                )),
                tool("get_product", "Obtiene un producto por id", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "id", Map.of("type", "integer", "minimum", 1)
                        ),
                        "required", List.of("id")
                )),
                tool("create_product", "Crea un producto", productRequestSchema()),
                tool("update_product", "Actualiza un producto", Map.of(
                        "type", "object",
                        "properties", new LinkedHashMap<>(Map.of(
                                "id", Map.of("type", "integer", "minimum", 1),
                                "name", Map.of("type", "string", "minLength", 2, "maxLength", 100),
                                "description", Map.of("type", "string", "maxLength", 500),
                                "price", Map.of("type", "number", "exclusiveMinimum", 0),
                                "stock", Map.of("type", "integer", "minimum", 0)
                        )),
                        "required", List.of("id", "name", "price", "stock")
                )),
                tool("delete_product", "Elimina un producto por id", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "id", Map.of("type", "integer", "minimum", 1)
                        ),
                        "required", List.of("id")
                )),
                tool("health_check", "Devuelve el estado del servicio", Map.of(
                        "type", "object",
                        "properties", Map.of()
                ))
        ));
    }

    public McpToolResponse callTool(String toolName, Map<String, Object> arguments) {
        try {
            return switch (toolName) {
                case "list_products" -> listProducts(arguments);
                case "get_product" -> getProduct(arguments);
                case "create_product" -> createProduct(arguments);
                case "update_product" -> updateProduct(arguments);
                case "delete_product" -> deleteProduct(arguments);
                case "health_check" -> healthCheck();
                default -> throw new McpProtocolException(McpErrorCode.METHOD_NOT_FOUND,
                        "Tool no encontrada: " + toolName);
            };
        } catch (ResourceNotFoundException ex) {
            return toolError("NOT_FOUND", ex.getMessage());
        }
    }

    private McpToolResponse listProducts(Map<String, Object> arguments) {
        ListProductsToolInput input = objectMapper.convertValue(arguments, ListProductsToolInput.class);
        validationUtils.validate(input);

        int page = input.page() == null ? 0 : input.page();
        int size = input.size() == null ? 10 : input.size();

        Page<ProductResponseDTO> result = productUseCase.listarProductos(PageRequest.of(page, size));
        Map<String, Object> structured = Map.of(
                "items", result.getContent(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
        return toolSuccess("Productos listados correctamente", structured);
    }

    private McpToolResponse getProduct(Map<String, Object> arguments) {
        GetProductToolInput input = objectMapper.convertValue(arguments, GetProductToolInput.class);
        validationUtils.validate(input);

        ProductResponseDTO product = productUseCase.obtenerProducto(input.id());
        return toolSuccess("Producto obtenido correctamente", Map.of("product", product));
    }

    private McpToolResponse createProduct(Map<String, Object> arguments) {
        ProductRequestDTO input = objectMapper.convertValue(arguments, ProductRequestDTO.class);
        validationUtils.validate(input);

        ProductResponseDTO product = productUseCase.crearProducto(input);
        return toolSuccess("Producto creado correctamente", Map.of("product", product));
    }

    private McpToolResponse updateProduct(Map<String, Object> arguments) {
        Long id = numberToLong(arguments.get("id"));
        if (id == null) {
            throw new McpProtocolException(McpErrorCode.INVALID_PARAMS, "El id es obligatorio");
        }

        ProductRequestDTO input = objectMapper.convertValue(arguments, ProductRequestDTO.class);
        validationUtils.validate(input);

        ProductResponseDTO product = productUseCase.actualizarProducto(id, input);
        return toolSuccess("Producto actualizado correctamente", Map.of("product", product));
    }

    private McpToolResponse deleteProduct(Map<String, Object> arguments) {
        DeleteProductToolInput input = objectMapper.convertValue(arguments, DeleteProductToolInput.class);
        validationUtils.validate(input);

        productUseCase.eliminarProducto(input.id());
        return toolSuccess("Producto eliminado correctamente", Map.of("deleted", true, "id", input.id()));
    }

    private McpToolResponse healthCheck() {
        return toolSuccess("Servicio activo", Map.of(
                "status", "UP",
                "service", "SmartInventory MCP"
        ));
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> inputSchema) {
        return Map.of(
                "name", name,
                "title", name,
                "description", description,
                "inputSchema", inputSchema
        );
    }

    private Map<String, Object> productRequestSchema() {
        return Map.of(
                "type", "object",
                "properties", new LinkedHashMap<>(Map.of(
                        "name", Map.of("type", "string", "minLength", 2, "maxLength", 100),
                        "description", Map.of("type", "string", "maxLength", 500),
                        "price", Map.of("type", "number", "exclusiveMinimum", 0),
                        "stock", Map.of("type", "integer", "minimum", 0)
                )),
                "required", List.of("name", "price", "stock")
        );
    }

    private McpToolResponse toolSuccess(String message, Map<String, Object> structuredContent) {
        return new McpToolResponse(
                List.of(Map.of("type", "text", "text", message)),
                structuredContent,
                false
        );
    }

    private McpToolResponse toolError(String code, String message) {
        return new McpToolResponse(
                List.of(Map.of("type", "text", "text", message)),
                Map.of("code", code, "message", message),
                true
        );
    }

    private Long numberToLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
