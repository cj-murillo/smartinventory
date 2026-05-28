package ec.org.cedia.smartinventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

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