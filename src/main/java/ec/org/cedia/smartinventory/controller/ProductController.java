package ec.org.cedia.smartinventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> crearProducto(
            @Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.status(201).body(productService.crearProducto(request));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> listarProductos() {
        return ResponseEntity.ok(productService.listarProductos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> obtenerProducto(@PathVariable Long id) {
        return ResponseEntity.ok(productService.obtenerProducto(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.ok(productService.actualizarProducto(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}