package com.newwork.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coreApi() {
        var bearer = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste token from /auth/login (without the word 'Bearer').");

        return new OpenAPI()
                .info(new Info()
                        .title("NEWWORK Core API")
                        .version("v1")
                        .description("Employees, Profiles, Feedback, Absences"))
                .components(new Components().addSecuritySchemes("bearerAuth", bearer))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean GroupedOpenApi employeesApi() {
        return GroupedOpenApi.builder().group("employees").pathsToMatch("/api/employees/**").build();
    }
    @Bean GroupedOpenApi profilesApi() {
        return GroupedOpenApi.builder().group("profiles").pathsToMatch("/api/employees/*/profile/**").build();
    }
    @Bean GroupedOpenApi feedbackApi() {
        return GroupedOpenApi.builder().group("feedback").pathsToMatch("/api/employees/*/feedback/**").build();
    }
    @Bean GroupedOpenApi absencesApi() {
        return GroupedOpenApi.builder().group("absences").pathsToMatch("/api/absences/**", "/api/employees/*/absences/**").build();
    }
    @Bean GroupedOpenApi authApi() {
        return GroupedOpenApi.builder().group("auth").pathsToMatch("/auth/**").build();
    }
}
