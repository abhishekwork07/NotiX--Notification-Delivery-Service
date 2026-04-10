package com.abhishek.notix.api_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("X-API-KEY", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-KEY"))
                        .addSecuritySchemes("X-NOTIX-API-KEY", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-NOTIX-API-KEY"))
                        .addSecuritySchemes("X-NOTIX-BOOTSTRAP-KEY", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-NOTIX-BOOTSTRAP-KEY")))
                .info(new Info()
                        .title("NotiX API Service")
                        .version("2.0")
                        .description("Public v1/v2 API, authentication, tenant control plane, notification intake, schedules, usage, and webhooks.")
                );
    }
}
