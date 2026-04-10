package com.abhishek.notix.email_sender_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NotiX Email Sender Service")
                        .version("2.0")
                        .description("Email worker service that consumes email notifications, records attempts, and emits delivery status events."));
    }
}
