package ec.org.cedia.smartinventory.config;

import ec.org.cedia.smartinventory.mcp.McpToolService;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public McpJsonMapper mcpJsonMapper() {
        return new JacksonMcpJsonMapperSupplier().get();
    }

    @Bean
    public HttpServletStreamableServerTransportProvider mcpTransportProvider(McpJsonMapper mcpJsonMapper) {
        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(mcpJsonMapper)
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServlet(
            HttpServletStreamableServerTransportProvider transportProvider) {
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> servlet =
                new ServletRegistrationBean<>(transportProvider, "/mcp", "/mcp/*");
        servlet.setName("mcpServlet");
        servlet.setLoadOnStartup(1);
        return servlet;
    }

    @Bean(destroyMethod = "closeGracefully")
    public McpSyncServer mcpServer(
            HttpServletStreamableServerTransportProvider transportProvider,
            McpJsonMapper mcpJsonMapper,
            McpToolService mcpToolService) {
        return McpServer.sync(transportProvider)
                .serverInfo("smartinventory-mcp", "0.1.0")
                .instructions("Usa las tools de productos para consultar y modificar el inventario.")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .jsonMapper(mcpJsonMapper)
                .tools(mcpToolService.toolSpecifications())
                .build();
    }
}
