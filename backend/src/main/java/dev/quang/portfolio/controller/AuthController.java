package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.PortfolioDtos.LoginRequest;
import dev.quang.portfolio.dto.PortfolioDtos.LoginResponse;
import dev.quang.portfolio.security.JwtService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPasswordHash;

    public AuthController(
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.password:admin123}") String adminPassword
    ) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPasswordHash = adminPassword.startsWith("$2")
                ? adminPassword
                : passwordEncoder.encode(adminPassword);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        boolean validUser = adminUsername.equals(request.username().trim());
        boolean validPassword = passwordEncoder.matches(request.password(), adminPasswordHash);
        if (!validUser || !validPassword) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        String token = jwtService.generateToken(adminUsername);
        return new LoginResponse(token, adminUsername, jwtService.getExpirationMs());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "username", authentication.getName(),
                "role", "ROLE_ADMIN"
        );
    }
}
