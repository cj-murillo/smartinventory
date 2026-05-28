package ec.org.cedia.smartinventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ObjectMapper mcpObjectMapper() {
        return new ObjectMapper();
    }
}
