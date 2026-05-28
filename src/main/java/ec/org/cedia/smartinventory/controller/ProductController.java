package ec.org.cedia.smartinventory.controller;

import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Gestión del catálogo de productos de SmartInventory")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Listar productos", description = "Retorna todos los productos con paginación")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente") })
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> listarProductos(
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(productService.listarProductos(pageable));
    }

    @Operation(summary = "Obtener producto por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto encontrado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> obtenerProducto(
            @Parameter(description = "ID del producto") @PathVariable Long id) {
        return ResponseEntity.ok(productService.obtenerProducto(id));
    }

    @Operation(summary = "Crear producto")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Producto creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<ProductResponseDTO> crearProducto(
            @Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.status(201).body(productService.crearProducto(request));
    }

    @Operation(summary = "Actualizar producto")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto actualizado"),
        @ApiResponse(responseCode = "404", description = "No encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> actualizarProducto(
            @Parameter(description = "ID del producto") @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.ok(productService.actualizarProducto(id, request));
    }

    @Operation(summary = "Eliminar producto")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado correctamente"),
        @ApiResponse(responseCode = "404", description = "No encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @Parameter(description = "ID del producto") @PathVariable Long id) {
        productService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}