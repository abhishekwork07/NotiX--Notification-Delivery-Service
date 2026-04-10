package com.abhishek.notix.sms_sender_service.config;

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
                        .title("NotiX SMS Sender Service")
                        .version("2.0")
                        .description("SMS worker service that consumes SMS notifications, sends through Twilio/provider config, records attempts, and emits status events."));
    }
}
