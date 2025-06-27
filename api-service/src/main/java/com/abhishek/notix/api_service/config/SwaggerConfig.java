package com.abhishek.notix.api_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .openapi("3.0.n") // 🔑 Force OpenAPI 3.0.x
                .info(new Info()
                        .title("NotiX Notification API")
                        .version("1.0")
                        .description("API documentation for the NotiX notification delivery system")
                        .contact(new Contact().name("Abhishek Gupta").email("you@example.com"))
                );
    }
}
