package javax.edi.service.config;

import javax.edi.service.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runs on startup to seed default admin user if none exists.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private AuthService authService;

    @Override
    public void run(String... args) {
        authService.ensureDefaultAdmin();
    }
}
