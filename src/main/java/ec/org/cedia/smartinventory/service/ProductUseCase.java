package ec.org.cedia.smartinventory.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;

public interface ProductUseCase {

    ProductResponseDTO crearProducto(ProductRequestDTO request);

    Page<ProductResponseDTO> listarProductos(Pageable pageable);

    ProductResponseDTO obtenerProducto(Long id);

    ProductResponseDTO actualizarProducto(Long id, ProductRequestDTO request);

    void eliminarProducto(Long id);
}
