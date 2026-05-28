# SmartInventory

API de inventario construida con Spring Boot 4. Expone dos formas de acceso sobre la misma lógica de negocio:

- REST API para clientes HTTP tradicionales
- MCP sobre `Streamable HTTP` para clientes compatibles con Model Context Protocol

## Qué incluye

- CRUD de productos
- Validación de entrada con Bean Validation
- Manejo centralizado de errores
- Seguridad básica con Spring Security
- Swagger/OpenAPI para explorar la API REST
- Persistencia con Spring Data JPA
- Perfil de desarrollo con PostgreSQL
- Perfil de tests con H2 en memoria
- Endpoint MCP con tools de productos

## Tecnologías

- Java 25
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL
- H2
- Lombok
- springdoc OpenAPI
- Maven Wrapper

## Estructura actual

```text
src/main/java/ec/org/cedia/smartinventory
├── config        Configuración de seguridad, OpenAPI y MCP
├── controller    Endpoints REST y endpoint MCP
├── dto           DTOs de entrada y salida REST
├── exception     Excepciones y manejador global
├── mcp           Lógica del adaptador MCP y tools
├── model         Entidades JPA
├── repository    Repositorios JPA
└── service       Lógica de negocio compartida
```

## Requisitos

- JDK 25 instalado
- PostgreSQL disponible para el perfil `dev`
- Puerto `8090` libre

Verifica Java:

```bash
java -version
```

## Configuración

El proyecto usa por defecto:

- puerto `8090`
- context path `/api`
- perfil activo `dev`

Eso está definido en `src/main/resources/application.properties`.

### Variables de entorno para desarrollo

El perfil `dev` usa PostgreSQL y admite estas variables:

```bash
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

La URL de desarrollo viene fija así:

```text
jdbc:postgresql://localhost:5439/smartinventory
```

Si tu PostgreSQL corre en otro puerto o con otro nombre de base, ajusta [application-dev.properties](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/main/resources/application-dev.properties:1).

### Perfil de tests

Los tests usan H2 en memoria mediante [application-test.properties](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/test/resources/application-test.properties:1), así que no dependen de PostgreSQL.

## Cómo levantar el proyecto

### Opción 1: usando Maven Wrapper

```bash
./mvnw spring-boot:run
```

### Opción 2: compilar y ejecutar el jar

```bash
./mvnw clean package
java -jar target/smartinventory-0.0.1-SNAPSHOT.jar
```

Si todo arranca bien, la app queda disponible en:

```text
http://localhost:8090/api
```

## Cómo correr tests

```bash
./mvnw test
```

Actualmente la suite cubre:

- health REST
- CRUD REST
- seguridad REST
- flujo MCP básico

## REST API

Base URL:

```text
http://localhost:8090/api
```

### Endpoints principales

- `GET /health`
- `GET /products`
- `GET /products/{id}`
- `POST /products`
- `PUT /products/{id}`
- `DELETE /products/{id}`

### Seguridad REST

Rutas públicas:

- `GET /api/health`
- `GET /api/products`
- `GET /api/products/{id}`
- Swagger

Rutas protegidas:

- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`

Usuario en memoria para desarrollo:

```text
username: admin
password: admin123
```

### Ejemplos REST

Listar productos:

```bash
curl http://localhost:8090/api/products
```

Obtener health:

```bash
curl http://localhost:8090/api/health
```

Crear producto:

```bash
curl -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell",
    "description": "Equipo de oficina",
    "price": 999.99,
    "stock": 5
  }' \
  http://localhost:8090/api/products
```

Actualizar producto:

```bash
curl -u admin:admin123 \
  -X PUT \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell X",
    "description": "16GB RAM",
    "price": 1099.99,
    "stock": 4
  }' \
  http://localhost:8090/api/products/1
```

Eliminar producto:

```bash
curl -u admin:admin123 \
  -X DELETE \
  http://localhost:8090/api/products/1
```

## Swagger / OpenAPI

Swagger UI está disponible en:

```text
http://localhost:8090/api/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8090/api/v3/api-docs
```

## MCP por Streamable HTTP

El proyecto expone MCP en:

```text
http://localhost:8090/api/mcp
```

### Estado actual del endpoint MCP

La implementación actual soporta:

- `initialize`
- `notifications/initialized`
- `tools/list`
- `tools/call`
- `DELETE /api/mcp` para cerrar sesión

`GET /api/mcp` responde `405`, así que hoy no hay stream SSE abierto. El transporte útil es `POST` sobre `Streamable HTTP`.

### Tools disponibles

- `list_products`
- `get_product`
- `create_product`
- `update_product`
- `delete_product`
- `health_check`

### Versiones MCP aceptadas

- `2025-11-25`
- `2025-06-18`

## Cómo conectarte con MCP Inspector

Levanta primero la app:

```bash
./mvnw spring-boot:run
```

Luego abre el Inspector:

```bash
npx @modelcontextprotocol/inspector
```

En la interfaz del Inspector:

- selecciona `Streamable HTTP`
- usa como URL:

```text
http://localhost:8090/api/mcp
```

### Configuración ejemplo para Inspector

```json
{
  "mcpServers": {
    "smartinventory": {
      "type": "streamable-http",
      "url": "http://localhost:8090/api/mcp"
    }
  }
}
```

## Ejemplo de flujo MCP

### 1. `initialize`

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-11-25",
    "capabilities": {},
    "clientInfo": {
      "name": "inspector-client",
      "version": "1.0.0"
    }
  }
}
```

El servidor devolverá un header `Mcp-Session-Id`. Ese valor debe enviarse después en las siguientes llamadas.

### 2. `tools/list`

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```

Headers requeridos:

```text
Mcp-Session-Id: <session-id>
MCP-Protocol-Version: 2025-11-25
```

### 3. `tools/call` para crear producto

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "create_product",
    "arguments": {
      "name": "Mouse MCP",
      "description": "Mouse de prueba",
      "price": 25.5,
      "stock": 7
    }
  }
}
```

## Diseño interno

REST y MCP no duplican lógica de negocio.

La relación actual es:

```text
REST Controller  ---> ProductUseCase <--- MCP Tool Service
                           |
                      ProductService
                           |
                    ProductRepository
                           |
                         DB
```

Puntos clave:

- `ProductService` implementa `ProductUseCase`
- `ProductController` usa `ProductUseCase`
- `McpToolService` usa `ProductUseCase`
- así REST y MCP comparten exactamente la misma lógica principal

## Archivos importantes

- [pom.xml](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/pom.xml:1)
- [application.properties](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/main/resources/application.properties:1)
- [application-dev.properties](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/main/resources/application-dev.properties:1)
- [SecurityConfig.java](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/main/java/ec/org/cedia/smartinventory/config/SecurityConfig.java:1)
- [ProductController.java](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/main/java/ec/org/cedia/smartinventory/controller/ProductController.java:1)
- [McpController.java](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/main/java/ec/org/cedia/smartinventory/controller/McpController.java:1)
- [McpToolService.java](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/main/java/ec/org/cedia/smartinventory/mcp/McpToolService.java:1)
- [ProductService.java](/home/cedia/Documentos/CEDIA-CURSOS/Programación%20de%20Backend%20%2805-26%29/smartinventory/src/main/java/ec/org/cedia/smartinventory/service/ProductService.java:1)

## Limitaciones actuales

- La capa MCP fue implementada manualmente como primer paso.
- A futuro conviene migrarla al SDK oficial de `modelcontextprotocol/java-sdk`.
- `GET /api/mcp` no abre stream SSE todavía.
- La autenticación MCP aún no está endurecida para un despliegue remoto real.

## Próximos pasos razonables

- migrar MCP al SDK oficial
- agregar autenticación/autorización para MCP
- soportar más services y más tools
- estabilizar el modelo paginado REST si quieres un contrato JSON más estricto

