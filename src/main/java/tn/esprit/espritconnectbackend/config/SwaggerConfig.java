package tn.esprit.espritconnectbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI espritConnectOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EspritConnect API Documentation")
                        .description("Documentation complète de l'API EspritConnect - Plateforme de réseau social pour les étudiants et anciens élèves d'Esprit")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EspritConnect Team")
                                .email("contact@espritconnect.tn")
                                .url("https://www.espritconnect.tn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

