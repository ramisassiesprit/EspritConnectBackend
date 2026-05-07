package tn.esprit.espritconnectbackend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Esprit Connect Team",
                        email = "contact@esprit.tn",
                        url = "https://espritconnect.com"
                ),
                description = "Documentation de l'API Backend pour la plateforme Esprit Connect",
                title = "Esprit Connect API",
                version = "1.0"
        ),
        servers = {
                @Server(
                        description = "Environnement de développement (Local)",
                        url = "http://localhost:8086/EspritConnect"
                )
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Authentification JWT. Entrez simplement votre token Bearer ci-dessous.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
