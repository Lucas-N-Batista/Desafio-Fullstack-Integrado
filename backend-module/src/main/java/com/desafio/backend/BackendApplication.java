package com.desafio.backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Ponto de entrada da aplicação Spring Boot.
 *
 * <p>Swagger UI disponível em: {@code http://localhost:8080/swagger-ui.html}</p>
 */
@SpringBootApplication
@EntityScan(basePackages = {
    "com.desafio.ejb.model",       // Entidade Beneficio do ejb-module
    "com.desafio.backend"           // Futuras entidades do próprio backend
})
@EnableJpaRepositories(basePackages = "com.desafio.backend.repository")
@OpenAPIDefinition(
        info = @Info(
                title       = "Desafio Fullstack Integrado – API de Benefícios",
                version     = "1.0.0",
                description = "CRUD de Benefícios e transferência de saldo com locking e validações",
                contact     = @Contact(name = "Desafio Fullstack", email = "dev@desafio.com")
        )
)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
