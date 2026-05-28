package ec.org.cedia.smartinventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class McpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void initialize_toolsList_y_createProduct_debeFuncionar() throws Exception {
        String initializeBody = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-06-18",
                    "capabilities": {},
                    "clientInfo": {
                      "name": "test-client",
                      "version": "1.0.0"
                    }
                  }
                }
                """;

        String sessionId = mockMvc.perform(post("/api/mcp")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(initializeBody))
                .andExpect(status().isOk())
                .andExpect(header().exists("Mcp-Session-Id"))
                .andExpect(header().string("MCP-Protocol-Version", "2025-06-18"))
                .andExpect(jsonPath("$.result.capabilities.tools.listChanged").value(false))
                .andReturn()
                .getResponse()
                .getHeader("Mcp-Session-Id");

        mockMvc.perform(post("/api/mcp")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Mcp-Session-Id", sessionId)
                        .header("MCP-Protocol-Version", "2025-06-18")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "method": "notifications/initialized"
                                }
                                """))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/mcp")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Mcp-Session-Id", sessionId)
                        .header("MCP-Protocol-Version", "2025-06-18")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 2,
                                  "method": "tools/list",
                                  "params": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("create_product")))
                .andExpect(jsonPath("$.result.tools[*].name", hasItem("list_products")));

        mockMvc.perform(post("/api/mcp")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Mcp-Session-Id", sessionId)
                        .header("MCP-Protocol-Version", "2025-06-18")
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.structuredContent.product.name").value("Mouse MCP"))
                .andExpect(jsonPath("$.result.structuredContent.product.id").exists());
    }

    @Test
    void createProduct_conParametrosInvalidos_debeRetornarErrorMcp() throws Exception {
        String sessionId = mockMvc.perform(post("/api/mcp")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 1,
                                  "method": "initialize",
                                  "params": {
                                    "protocolVersion": "2025-06-18",
                                    "capabilities": {},
                                    "clientInfo": {
                                      "name": "test-client",
                                      "version": "1.0.0"
                                    }
                                  }
                                }
                                """))
                .andReturn()
                .getResponse()
                .getHeader("Mcp-Session-Id");

        mockMvc.perform(post("/api/mcp")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Mcp-Session-Id", sessionId)
                        .header("MCP-Protocol-Version", "2025-06-18")
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 10,
                                  "method": "tools/call",
                                  "params": {
                                    "name": "create_product",
                                    "arguments": {
                                      "price": 10.0,
                                      "stock": 2
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.data.fields.name").exists());
    }

    @Test
    void get_mcp_debeRetornar405_y_delete_debeCerrarSesion() throws Exception {
        mockMvc.perform(get("/api/mcp").contextPath("/api"))
                .andExpect(status().isMethodNotAllowed());

        String sessionId = mockMvc.perform(post("/api/mcp")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 1,
                                  "method": "initialize",
                                  "params": {
                                    "protocolVersion": "2025-06-18",
                                    "capabilities": {},
                                    "clientInfo": {
                                      "name": "test-client",
                                      "version": "1.0.0"
                                    }
                                  }
                                }
                                """))
                .andReturn()
                .getResponse()
                .getHeader("Mcp-Session-Id");

        mockMvc.perform(delete("/api/mcp")
                        .contextPath("/api")
                        .header("Mcp-Session-Id", sessionId))
                .andExpect(status().isNoContent());
    }

    @Test
    void initialize_conVersion2025_11_25_debeSerAceptada() throws Exception {
        mockMvc.perform(post("/api/mcp")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "id": 99,
                                  "method": "initialize",
                                  "params": {
                                    "protocolVersion": "2025-11-25",
                                    "capabilities": {},
                                    "clientInfo": {
                                      "name": "inspector-client",
                                      "version": "0.17.2"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("MCP-Protocol-Version", "2025-11-25"))
                .andExpect(jsonPath("$.result.protocolVersion").value("2025-11-25"));
    }
}
