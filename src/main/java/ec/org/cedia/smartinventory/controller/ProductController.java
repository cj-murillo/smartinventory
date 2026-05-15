package ec.org.cedia.smartinventory.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.org.cedia.smartinventory.model.Product;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    // Datos en memoria — temporal, solo para esta sesión
    private final List<Product> products = List.of(
        Product.builder().id(1L).name("Laptop")
            .description("Laptop de alto rendimiento")
            .price(1200.0).stock(10).active(true).build(),
        Product.builder().id(2L).name("Mouse")
            .description("Mouse inalámbrico")
            .price(25.5).stock(50).active(true).build(),
        Product.builder().id(3L).name("Teclado")
            .description("Teclado mecánico")
            .price(45.0).stock(30).active(true).build()
    );

    // GET /api/products
    @GetMapping
    public List<Product> listar() {
        return products;
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public Product buscarPorId(@PathVariable Long id) {
        return products.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}