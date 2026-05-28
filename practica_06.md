# Práctica — Sesión 7
## Swagger/OpenAPI + Logging + Buenas Prácticas + Variables de Entorno + Tests de Integración
### SmartInventory API — Spring Boot 4.0.6

> **Punto de partida:** SmartInventory con Spring Security y 10 tests en verde (commit sesión 6).
> **Tiempo estimado:** 60 minutos de práctica

---

## Antes de empezar

- [ ] `mvn spring-boot:run` levanta sin errores
- [ ] `mvn test` corre con 10 tests en verde
- [ ] GET `http://localhost:8080/api/products` responde 200

---

## PARTE A — Swagger/OpenAPI (35 min)

---

## Paso 1 — Agregar springdoc-openapi para Spring Boot 4

En Spring Boot 4, springdoc-openapi tiene su propia versión 3.x. Agrega en `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.3</version>
</dependency>
```

> **¿Por qué versión 3.x y no 2.x?**
> springdoc-openapi 2.x es para Spring Boot 3. La versión 3.x es compatible con Spring Boot 4 y Jackson 3 que viene incluido en SB4.

Reinicia con `mvn spring-boot:run` y abre:

```
http://localhost:8080/swagger-ui/index.html
```

> **Nota importante en Spring Boot 4:** la URL cambió. Ya no es `/swagger-ui.html` — ahora es `/swagger-ui/index.html`. También `/v3/api-docs` y `/swagger-ui.html` están **desactivados por defecto** en springdoc 3.x. Los activamos en el siguiente paso.

---

## Paso 2 — Habilitar Swagger UI y permitirlo en SecurityConfig

Primero activa las rutas en `application.properties`:

```properties
# Habilitar Swagger UI y api-docs (desactivados por defecto en springdoc 3.x)
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
```

Luego actualiza `SecurityConfig.java` para permitir acceso público a Swagger:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(org.springframework.http.HttpMethod.GET,
                "/products", "/products/**", "/health").permitAll()
            .requestMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui/index.html"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .httpBasic(basic -> {});

    return http.build();
}
```

> **Regla importante:** como `/api` ya está definido en `server.servlet.context-path`, **no se repite** en `@RequestMapping(...)` ni en `requestMatchers(...)`.  
> Ejemplo: `@RequestMapping("/products")` se expone finalmente como `/api/products`.

Reinicia y abre `http://localhost:8080/swagger-ui/index.html` — deberías ver la UI con los endpoints de SmartInventory.

---

## Paso 3 — Configuración de OpenAPI

Crea `OpenApiConfig.java` en el paquete `config`:

```java
package ec.org.cedia.smartinventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartInventoryOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SmartInventory API")
                .description("API REST para gestión de inventario de productos")
                .version("v1.0")
                .contact(new Contact()
                    .name("CEDIA")
                    .email("soporte@cedia.org.ec")
                )
            );
    }
}
```

---

## Paso 4 — Anotar ProductController con paginación

Abre `ProductController.java` y reemplaza su contenido:

```java
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> listarProductos(
            @ParameterObject Pageable pageable) {   // ← @ParameterObject para que Swagger muestre page/size/sort
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
        @ApiResponse(responseCode = "201", description = "Producto creado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping
    public ResponseEntity<ProductResponseDTO> crearProducto(
            @Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.status(201).body(productService.crearProducto(request));
    }

    @Operation(summary = "Actualizar producto")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto actualizado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> actualizarProducto(
            @Parameter(description = "ID del producto") @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO request) {
        return ResponseEntity.ok(productService.actualizarProducto(id, request));
    }

    @Operation(summary = "Eliminar producto")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Producto eliminado correctamente"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @Parameter(description = "ID del producto") @PathVariable Long id) {
        productService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}
```

> **`@ParameterObject` en lugar de `@Parameter`** para `Pageable` — springdoc 3.x requiere esta anotación para descomponer `Pageable` en los parámetros `page`, `size` y `sort` visibles en Swagger UI.

---

## Paso 5 — Anotar HealthController

```java
package ec.org.cedia.smartinventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Estado del servicio")
public class HealthController {

    @Operation(summary = "Health check",
               description = "Verifica que la API está levantada y respondiendo")
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString(),
            "service", "SmartInventory API"
        ));
    }
}
```

---

## Checkpoint A ✅

Abre `http://localhost:8080/swagger-ui/index.html` y verifica:

- [ ] Dos grupos: **Productos** y **Health**
- [ ] Cada endpoint tiene descripción y códigos de respuesta
- [ ] GET /api/products muestra los parámetros `page`, `size`, `sort`
- [ ] Puedes ejecutar un GET desde la UI y ver la respuesta

---

## PARTE B — Paginación en el Service (5 min)

---

## Paso 6 — Actualizar ProductService

Agrega los imports y actualiza `listarProductos`:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public Page<ProductResponseDTO> listarProductos(Pageable pageable) {
    return productRepository.findAll(pageable).map(this::toResponseDTO);
}
```

Prueba en Postman:

```
GET http://localhost:8080/api/products?page=0&size=5&sort=name,asc
```

---

## PARTE C — Logging con SLF4J (15 min)

---

## Paso 7 — @Slf4j en ProductService

Reemplaza el contenido completo de `ProductService.java`:

```java
package ec.org.cedia.smartinventory.service;

import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import ec.org.cedia.smartinventory.dto.ProductResponseDTO;
import ec.org.cedia.smartinventory.exception.ResourceNotFoundException;
import ec.org.cedia.smartinventory.model.Product;
import ec.org.cedia.smartinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponseDTO crearProducto(ProductRequestDTO request) {
        log.info("Creando producto: nombre='{}', precio={}", request.getName(), request.getPrice());
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .active(true)
                .build();
        Product saved = productRepository.save(product);
        log.info("Producto creado exitosamente con id={}", saved.getId());
        return toResponseDTO(saved);
    }

    public Page<ProductResponseDTO> listarProductos(Pageable pageable) {
        log.debug("Listando productos — página={}, tamaño={}", pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAll(pageable).map(this::toResponseDTO);
    }

    public ProductResponseDTO obtenerProducto(Long id) {
        log.debug("Buscando producto con id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado con id={}", id);
                    return new ResourceNotFoundException("Producto no encontrado con id: " + id);
                });
        return toResponseDTO(product);
    }

    public ProductResponseDTO actualizarProducto(Long id, ProductRequestDTO request) {
        log.info("Actualizando producto id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Intento de actualizar producto inexistente id={}", id);
                    return new ResourceNotFoundException("Producto no encontrado con id: " + id);
                });
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        log.info("Producto id={} actualizado correctamente", id);
        return toResponseDTO(productRepository.save(product));
    }

    public void eliminarProducto(Long id) {
        log.info("Eliminando producto id={}", id);
        if (!productRepository.existsById(id)) {
            log.warn("Intento de eliminar producto inexistente id={}", id);
            throw new ResourceNotFoundException("Producto no encontrado con id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Producto id={} eliminado correctamente", id);
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
```

---

## Paso 8 — Logging en GlobalExceptionHandler

Agrega `@Slf4j` y los logs en `GlobalExceptionHandler.java`:

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        log.warn("Error de validación: {}", ex.getBindingResult().getAllErrors());
        // ... resto del código igual
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        // ... resto del código igual
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Error interno no controlado: {}", ex.getMessage(), ex);
        // ... resto del código igual
    }
}
```

---

## Paso 9 — Niveles de log en application.properties

```properties
logging.level.ec.org.cedia.smartinventory=DEBUG
logging.level.org.springframework.security=INFO
logging.level.org.hibernate.SQL=DEBUG
logging.pattern.console=%d{HH:mm:ss} [%level] %logger{36} - %msg%n
```

Reinicia y haz requests desde Postman. Debes ver logs como:

```
10:30:15 [INFO]  e.o.c.s.service.ProductService - Creando producto: nombre='Laptop', precio=899.99
10:30:20 [WARN]  e.o.c.s.service.ProductService - Producto no encontrado con id=999
```

---

## Checkpoint B ✅

---

## PARTE D — Variables de Entorno (10 min)

---

## Paso 10 — Tres perfiles de Spring Boot

Crea o reemplaza los archivos en `src/main/resources/`:

**`application.properties`** — base sin credenciales:
```properties
spring.application.name=smartinventory
spring.profiles.active=dev
logging.pattern.console=%d{HH:mm:ss} [%level] %logger{36} - %msg%n
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
```

**`application-dev.properties`** — desarrollo con valores por defecto:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/smartinventory
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres123}
spring.jpa.hibernate.ddl-auto=update
logging.level.ec.org.cedia.smartinventory=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

**`application-prod.properties`** — producción sin valores por defecto:
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
logging.level.ec.org.cedia.smartinventory=INFO
logging.level.org.hibernate.SQL=WARN
```

> **La diferencia clave:** `${DB_PASSWORD:postgres123}` tiene un valor por defecto después de los dos puntos. `${DB_PASSWORD}` sin dos puntos — si la variable no existe, la app **no arranca**. En producción eso es intencional.

---

## Paso 10.1 — Cómo probar correctamente los perfiles

Haz estas 4 verificaciones:

### 1. Verificar que `dev` es el perfil por defecto

Ejecuta:

```bash
mvn spring-boot:run
```

Debes ver en el log algo como:

```text
The following 1 profile is active: "dev"
```

### 2. Verificar que `dev` funciona con valores por defecto

Sin definir `DB_USERNAME` ni `DB_PASSWORD`, vuelve a ejecutar:

```bash
mvn spring-boot:run
```

Si PostgreSQL local está disponible, la aplicación debe intentar conectarse usando los valores por defecto del perfil `dev`:

- usuario: `postgres`
- password: `postgres123`

### 3. Verificar que `dev` sí toma variables de entorno cuando existen

**Linux/Mac**
```bash
export DB_USERNAME=otro_usuario
export DB_PASSWORD=otra_clave
mvn spring-boot:run
```

**Windows PowerShell**
```powershell
$env:DB_USERNAME="otro_usuario"
$env:DB_PASSWORD="otra_clave"
mvn spring-boot:run
```

La aplicación debe intentar conectarse con esos valores y no con los defaults.

### 4. Verificar que `prod` falla si faltan variables obligatorias

Ejecuta:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

sin definir `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

La aplicación **debe fallar al arrancar**.

Luego define las variables y vuelve a ejecutar:

**Linux/Mac**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/smartinventory
export DB_USERNAME=postgres
export DB_PASSWORD=postgres123
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**Windows PowerShell**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/smartinventory"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres123"
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Ahora sí debe arrancar.

---

## Paso 11 — Proteger credenciales con .gitignore

Abre `.gitignore` en la raíz y agrega:

```
.env
application-prod.properties
```

---

## Paso 12 — Definir variables localmente

**Linux/Mac:**
```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres123
mvn spring-boot:run
```

**Windows PowerShell:**
```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres123"
mvn spring-boot:run
```

**En IntelliJ:** Run → Edit Configurations → Environment Variables → agregar `DB_USERNAME=postgres;DB_PASSWORD=postgres123`

---

## Checkpoint C ✅

Verifica en el log de inicio:

```
The following 1 profile is active: "dev"
```

---

## PARTE E — Tests de Integración (15 min)

---

## Paso 13 — Dependencia H2

Verifica o agrega en `pom.xml`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Paso 14 — Configuración de H2 para tests

Crea `src/test/resources/application-test.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
logging.level.ec.org.cedia.smartinventory=WARN
logging.level.org.hibernate.SQL=WARN
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

---

## Paso 15 — ProductIntegrationTest

Crea `src/test/java/ec/org/cedia/smartinventory/ProductIntegrationTest.java`:

```java
package ec.org.cedia.smartinventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.org.cedia.smartinventory.dto.ProductRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // TEST 1 — Health check end-to-end
    @Test
    void healthCheck_debeRetornarStatusUp() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    // TEST 2 — Crear y recuperar — flujo completo con BD real
    @Test
    void crearYRecuperarProducto_flujoCompleto() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName("Laptop Dell Integration");
        request.setPrice(999.99);
        request.setStock(5);

        String responseBody = mockMvc.perform(post("/api/products")
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Laptop Dell Integration"))
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();

        mockMvc.perform(get("/api/products/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Laptop Dell Integration"))
            .andExpect(jsonPath("$.active").value(true));
    }

    // TEST 3 — Validación end-to-end
    @Test
    void crearProducto_sinNombre_debeRetornar400() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setPrice(100.0);
        request.setStock(5);

        mockMvc.perform(post("/api/products")
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.name").exists());
    }

    // TEST 4 — 404 real desde la BD
    @Test
    void obtenerProducto_idInexistente_debeRetornar404() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}
```

---

## Paso 16 — Correr todos los tests

```bash
mvn clean test
```

Resultado esperado:

```
Tests run: 4, Failures: 0, Errors: 0  ← integración
Tests run: 10, Failures: 0, Errors: 0 ← unitarios sesión 6
BUILD SUCCESS
```

> **Nota:** el test `listarProductos_sinAuth_debeRetornar200` de sesión 6 puede fallar porque ahora devuelve `Page` en lugar de array. Actualiza el `andExpect` así:
> ```java
> .andExpect(jsonPath("$.content").isArray());
> ```

---

## Commit final

```bash
git add .
git commit -m "feat: Swagger 3.x + logging SLF4J + perfiles + variables de entorno + tests de integración H2"
```

---

## Diferencia entre los dos tipos de test

|            | @WebMvcTest (sesión 6) | @SpringBootTest (hoy) |
| ---------- | ---------------------- | --------------------- |
| BD         | Mockeada               | Real — H2 en memoria  |
| Service    | Mock                   | Real                  |
| Velocidad  | ~2 segundos            | ~10 segundos          |
| Qué valida | Controller aislado     | Flujo completo        |

---

## 📄 Entrega de evidencias

Arma un PDF con capturas de cada checkpoint en orden. Quien revise el PDF debe ver que cada paso funcionó.

**Capturas mínimas:**
- Swagger UI mostrando los endpoints con `page`, `size`, `sort` en GET /products
- Terminal con logs INFO y WARN al hacer requests
- `mvn test` con `BUILD SUCCESS` mostrando todos los tests

Nombra el archivo: `practica_06_[tu_nombre].pdf`

> **Fecha límite:** antes del inicio de la sesión 8.
