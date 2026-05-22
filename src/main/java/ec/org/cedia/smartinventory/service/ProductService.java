package ec.org.cedia.smartinventory.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.exception.ResourceNotFoundException;
import ec.org.cedia.smartinventory.model.Product;
import ec.org.cedia.smartinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

/**
 * Esta clase se encarga de gestionar las operaciones matemáticas básicas.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Crea un nuevo producto en el inventario.
     * 
     * @param request El objeto que contiene los datos del producto a crear.
     * @return El producto creado con su ID asignado.
     */
    public ProductResponseDTO crearProducto(ProductRequestDTO request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .active(true)
                .build();
        return toResponseDTO(productRepository.save(product));
    }

    public List<ProductResponseDTO> listarProductos() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ProductResponseDTO obtenerProducto(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        return toResponseDTO(product);
    }

    public ProductResponseDTO actualizarProducto(Long id, ProductRequestDTO request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return toResponseDTO(productRepository.save(product));
    }

    public void eliminarProducto(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponseDTO toResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .active(product.getActive())
                .build();
    }
}