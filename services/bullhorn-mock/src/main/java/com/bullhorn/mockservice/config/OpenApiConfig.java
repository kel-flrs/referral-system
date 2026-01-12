package com.bullhorn.mockservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI bullhornMockServiceOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Development server");

        Contact contact = new Contact();
        contact.setName("API Support");
        contact.setEmail("support@bullhorn-mock.com");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Bullhorn Mock Service API")
                .version("1.0.0")
                .description("Mock REST API service simulating Bullhorn CRM functionality with realistic test data. " +
                        "Provides endpoints for managing candidates, consultants, job orders, and submissions.\n\n" +
                        "**Authentication**: Most endpoints require OAuth 2.0 authentication. " +
                        "Use the BhRestToken (session token) obtained from the OAuth flow.\n\n" +
                        "**Quick Test**: You can use the pre-configured client credentials:\n" +
                        "- Client ID: `test-client-1`\n" +
                        "- Client Secret: `test-secret-1`\n" +
                        "- Username: `admin@bullhorn.local`\n" +
                        "- Password: `password123`")
                .contact(contact)
                .license(license);

        // Define security schemes
        SecurityScheme sessionTokenHeader = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("BhRestToken")
                .description("Bullhorn REST session token obtained from OAuth flow (Step 3). " +
                        "Paste your session token here to authenticate API requests.");

        SecurityScheme sessionTokenQuery = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.QUERY)
                .name("BhRestToken")
                .description("Bullhorn REST session token as query parameter");

        Components components = new Components()
                .addSecuritySchemes("BhRestToken", sessionTokenHeader)
                .addSecuritySchemes("BhRestToken-Query", sessionTokenQuery);

        // Add global security requirement (optional - can be overridden per endpoint)
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("BhRestToken");

        return new OpenAPI()
                .info(info)
                .servers(List.of(server))
                .components(components)
                .addSecurityItem(securityRequirement);
    }
}
