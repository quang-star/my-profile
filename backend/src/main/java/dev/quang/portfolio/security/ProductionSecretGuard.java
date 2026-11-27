package dev.quang.portfolio.security;

import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionSecretGuard implements ApplicationRunner {

    private final Environment environment;
    private final String jwtSecret;
    private final String adminPassword;
    private final String encryptionKey;

    public ProductionSecretGuard(
            Environment environment,
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.encryption.key:}") String encryptionKey
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret;
        this.adminPassword = adminPassword == null ? "" : adminPassword;
        this.encryptionKey = encryptionKey == null ? "" : encryptionKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        if (!isProd) {
            return;
        }
        if (jwtSecret.toLowerCase(Locale.ROOT).contains("change-this")) {
            throw new IllegalStateException(
                    "Refusing to start in production with default/placeholder app.jwt.secret containing 'change-this'"
            );
        }
        if ("admin123".equals(adminPassword)) {
            throw new IllegalStateException(
                    "Refusing to start in production with default app.admin.password ('admin123')"
            );
        }
        if (!encryptionKey.isBlank()) {
            String lowered = encryptionKey.toLowerCase(Locale.ROOT);
            if (lowered.contains("change-this")
                    || lowered.contains("replace-with")
                    || encryptionKey.trim().length() < 16) {
                throw new IllegalStateException(
                        "Refusing to start in production with weak or placeholder app.encryption.key"
                );
            }
        }
    }
}
