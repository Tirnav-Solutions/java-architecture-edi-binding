package javax.edi.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EDI-as-a-Service Platform.
 * 
 * Features:
 *   - EDI parsing, generation, validation (REST API)
 *   - 997 Functional Acknowledgement auto-generation
 *   - Trading partner onboarding and management
 *   - SFTP polling for inbound EDI files
 *   - Transaction logging (H2 file-based database)
 *   - Transaction history and dashboard API
 *   - API key authentication and rate limiting
 * 
 * Start with:
 *   mvn spring-boot:run -pl jaeb-service
 * 
 * Swagger UI:   http://localhost:8080/swagger-ui.html
 * H2 Console:   http://localhost:8080/h2-console
 * Health check: http://localhost:8080/actuator/health
 */
@SpringBootApplication
@EnableScheduling
public class EDIServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EDIServiceApplication.class, args);
    }
}
