package ec.org.cedia.smartinventory.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.org.cedia.smartinventory.config.SecurityConfig;
import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.exception.GlobalExceptionHandler;
import ec.org.cedia.smartinventory.exception.ResourceNotFoundException;
import ec.org.cedia.smartinventory.service.ProductService;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureJsonTesters
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ════════════════════════════════════════════════════
    // TEST 1 — Listar productos sin autenticación → 200
    // Valida: endpoint público, responde JSON, devuelve array
    // ════════════════════════════════════════════════════
    @Test
    void listarProductos_sinAuth_debeRetornar200() throws Exception {
        when(productService.listarProductos(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/products"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").isArray());
    }

    // ════════════════════════════════════════════════════
    // TEST 2 — Crear producto con datos válidos → 201
    // Valida: autenticación requerida, body correcto, id en response
    // ════════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void crearProducto_datosValidos_debeRetornar201() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Laptop Dell");
        request.setPrice(899.99);
        request.setStock(10);

        ProductResponseDTO response = ProductResponseDTO.builder()
            .id(1L)
            .name("Laptop Dell")
            .price(899.99)
            .stock(10)
            .active(true)
            .build();

        when(productService.crearProducto(any())).thenReturn(response);

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Laptop Dell"))
            .andExpect(jsonPath("$.active").value(true));
    }

    // ════════════════════════════════════════════════════
    // TEST 3 — Crear producto sin nombre → 400
    // Valida: @Valid activo, error de validación capturado
    // ════════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void crearProducto_sinNombre_debeRetornar400() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setPrice(100.0);
        request.setStock(5);

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists());
    }

    // ════════════════════════════════════════════════════
    // TEST 4 — Crear producto con precio negativo → 400
    // Valida: @DecimalMin activo, mensaje de error específico
    // ════════════════════════════════════════════════════
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void crearProducto_precioNegativo_debeRetornar400() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Producto");
        request.setPrice(-50.0);
        request.setStock(5);

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.price").exists());
    }

    // ════════════════════════════════════════════════════
    // TEST 5 — Crear producto sin autenticación → 401
    // Valida: Spring Security bloqueando la request
    // ════════════════════════════════════════════════════
    @Test
    void crearProducto_sinAutenticacion_debeRetornar401() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Producto");
        request.setPrice(100.0);
        request.setStock(5);

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    // ════════════════════════════════════════════════════
    // TEST 6 — Obtener producto por ID existente → 200
    // Valida: path variable, body correcto
    // ════════════════════════════════════════════════════
    @Test
    @WithMockUser
    void obtenerProducto_idExistente_debeRetornar200() throws Exception {
        ProductResponseDTO response = ProductResponseDTO.builder()
            .id(1L)
            .name("Laptop Dell")
            .price(899.99)
            .stock(10)
            .active(true)
            .build();

        when(productService.obtenerProducto(1L)).thenReturn(response);

        mockMvc.perform(get("/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Laptop Dell"));
    }

    // ════════════════════════════════════════════════════
    // TEST 7 — Obtener producto por ID inexistente → 404
    // Valida: GlobalExceptionHandler captura ResourceNotFoundException
    // ════════════════════════════════════════════════════
    @Test
    @WithMockUser
    void obtenerProducto_idInexistente_debeRetornar404() throws Exception {
        when(productService.obtenerProducto(999L))
            .thenThrow(new ResourceNotFoundException("Producto no encontrado con id: 999"));

        mockMvc.perform(get("/products/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Producto no encontrado con id: 999"));
    }

    // ════════════════════════════════════════════════════
    // TEST 8 — Eliminar sin autenticación → 401
    // Valida: Security protege DELETE igual que POST
    // ════════════════════════════════════════════════════
    @Test
    void eliminarProducto_sinAutenticacion_debeRetornar401() throws Exception {
        mockMvc.perform(delete("/products/1"))
            .andExpect(status().isUnauthorized());
    }
}
