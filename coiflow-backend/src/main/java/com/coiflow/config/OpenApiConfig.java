package com.coiflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coiflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Coiflow API")
                        .description("Salon management SaaS - REST API")
                        .version("0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .schemaRequirement("cookieAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("access_token"));
    }
}
