package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.MultiTenantDtos.AuthResponse;
import dev.quang.portfolio.dto.MultiTenantDtos.ChangePasswordRequest;
import dev.quang.portfolio.dto.MultiTenantDtos.FeatureFlagsDto;
import dev.quang.portfolio.dto.MultiTenantDtos.ForgotPasswordRequest;
import dev.quang.portfolio.dto.MultiTenantDtos.ForgotPasswordResponse;
import dev.quang.portfolio.dto.MultiTenantDtos.GoogleConfigDto;
import dev.quang.portfolio.dto.MultiTenantDtos.GoogleLoginRequest;
import dev.quang.portfolio.dto.MultiTenantDtos.RegisterRequest;
import dev.quang.portfolio.dto.MultiTenantDtos.ResetPasswordRequest;
import dev.quang.portfolio.dto.MultiTenantDtos.UpdateProfileRequest;
import dev.quang.portfolio.dto.MultiTenantDtos.UserProfileDto;
import dev.quang.portfolio.dto.PortfolioDtos.LoginRequest;
import dev.quang.portfolio.entity.PasswordResetTokenEntity;
import dev.quang.portfolio.entity.UserEntity;
import dev.quang.portfolio.repository.PageRepository;
import dev.quang.portfolio.repository.PasswordResetTokenRepository;
import dev.quang.portfolio.repository.UploadedFileRepository;
import dev.quang.portfolio.repository.UserRepository;
import dev.quang.portfolio.security.AuthenticatedUser;
import dev.quang.portfolio.security.JwtService;
import dev.quang.portfolio.service.FeatureFlagsService;
import dev.quang.portfolio.service.GoogleTokenVerifierService;
import dev.quang.portfolio.service.GoogleTokenVerifierService.GoogleUserInfo;
import dev.quang.portfolio.service.PasswordResetMailService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_-]{3,32}$");
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile("(?i)^(https?://|/api/files/).+");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "super-admin", "dashboard", "editor", "login", "register",
            "forgot-password", "reset-password",
            "api", "templates", "public", "u", "p", "auth", "files"
    );

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PageRepository pageRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetMailService passwordResetMailService;
    private final FeatureFlagsService featureFlagsService;

    public AuthController(
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            PageRepository pageRepository,
            UploadedFileRepository uploadedFileRepository,
            GoogleTokenVerifierService googleTokenVerifierService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetMailService passwordResetMailService,
            FeatureFlagsService featureFlagsService
    ) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.pageRepository = pageRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetMailService = passwordResetMailService;
        this.featureFlagsService = featureFlagsService;
    }

    @GetMapping("/google-config")
    public GoogleConfigDto googleConfig() {
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        boolean enabled = googleTokenVerifierService.isEnabled() && Boolean.TRUE.equals(flags.googleLoginEnabled());
        return new GoogleConfigDto(enabled, enabled ? googleTokenVerifierService.getClientId() : "");
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        if (Boolean.TRUE.equals(flags.maintenanceMode())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is currently under maintenance");
        }
        if (!Boolean.TRUE.equals(flags.registrationEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is temporarily disabled");
        }
        String username = validateAndNormalizeUsername(request.username());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setId("usr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        String displayName = request.displayName() != null && !request.displayName().isBlank()
                ? request.displayName().trim()
                : username;
        user.setDisplayName(displayName);
        user.setRole("USER");
        user.setMaxPages(3);
        user.setMaxStorageBytes(20L * 1024 * 1024);
        user.setStatus("ACTIVE");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername(), jwtService.getExpirationMs(), toProfileDto(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String identifier = request.username().trim();
        UserEntity user = userRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElse(null);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account uses Google Sign-In. Please continue with Google."
            );
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        if (Boolean.TRUE.equals(flags.maintenanceMode()) && !"SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is currently under maintenance");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername(), jwtService.getExpirationMs(), toProfileDto(user));
    }

    @PostMapping("/google")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        if (!Boolean.TRUE.equals(flags.googleLoginEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Google login is temporarily disabled");
        }
        GoogleUserInfo info = googleTokenVerifierService.verify(request.credential());
        String email = info.email().trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();

        boolean isNewUser = false;
        UserEntity user = userRepository.findByGoogleSub(info.sub()).orElse(null);

        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user != null) {
                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
                }
                if (Boolean.TRUE.equals(flags.maintenanceMode()) && !"SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is currently under maintenance");
                }
                user.setGoogleSub(info.sub());
                if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())
                        && info.picture() != null && !info.picture().isBlank()) {
                    user.setAvatarUrl(truncate(info.picture().trim(), 500));
                }
                user.setUpdatedAt(now);
                userRepository.save(user);
            } else {
                if (Boolean.TRUE.equals(flags.maintenanceMode())) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is currently under maintenance");
                }
                if (!Boolean.TRUE.equals(flags.registrationEnabled())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration is temporarily disabled");
                }
                isNewUser = true;
                String username = generateUniqueUsername(request.preferredUsername(), email);
                user = new UserEntity();
                user.setId("usr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                user.setUsername(username);
                user.setEmail(email);
                user.setPasswordHash(null);
                user.setGoogleSub(info.sub());
                if (info.picture() != null && !info.picture().isBlank()) {
                    user.setAvatarUrl(truncate(info.picture().trim(), 500));
                }
                String displayName = info.name() != null && !info.name().isBlank()
                        ? truncate(info.name().trim(), 120)
                        : username;
                user.setDisplayName(displayName);
                user.setRole("USER");
                user.setMaxPages(3);
                user.setMaxStorageBytes(20L * 1024 * 1024);
                user.setStatus("ACTIVE");
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                userRepository.save(user);
            }
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }
        if (Boolean.TRUE.equals(flags.maintenanceMode()) && !"SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is currently under maintenance");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername(), jwtService.getExpirationMs(), toProfileDto(user), isNewUser);
    }

    @GetMapping("/me")
    public UserProfileDto me(Authentication authentication) {
        AuthenticatedUser current = AuthenticatedUser.require(authentication);
        UserEntity user = userRepository.findById(current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return toProfileDto(user);
    }

    @PutMapping("/profile")
    public AuthResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        AuthenticatedUser current = AuthenticatedUser.require(authentication);
        UserEntity user = userRepository.findById(current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (request.username() != null) {
            String nextUsername = validateAndNormalizeUsername(request.username());
            if (!nextUsername.equalsIgnoreCase(user.getUsername())
                    && userRepository.existsByUsernameIgnoreCase(nextUsername)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }
            user.setUsername(nextUsername);
        }

        if (request.displayName() != null) {
            String nextDisplayName = request.displayName().trim();
            if (nextDisplayName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name cannot be blank");
            }
            user.setDisplayName(truncate(nextDisplayName, 120));
        }

        if (Boolean.TRUE.equals(request.clearAvatar())) {
            user.setAvatarUrl(null);
        } else if (request.avatarUrl() != null) {
            String nextAvatar = request.avatarUrl().trim();
            if (nextAvatar.isEmpty()) {
                user.setAvatarUrl(null);
            } else {
                if (!SAFE_URL_PATTERN.matcher(nextAvatar).matches()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Avatar URL must start with http://, https://, or /api/files/"
                    );
                }
                user.setAvatarUrl(truncate(nextAvatar, 500));
            }
        }

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername(), jwtService.getExpirationMs(), toProfileDto(user));
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        AuthenticatedUser current = AuthenticatedUser.require(authentication);
        UserEntity user = userRepository.findById(current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            if (request.currentPassword() == null
                    || request.currentPassword().isBlank()
                    || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return Map.of("message", "Password updated");
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        boolean smtpConfigured = passwordResetMailService.isSmtpConfigured();

        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user != null && "ACTIVE".equalsIgnoreCase(user.getStatus())) {
            Instant now = Instant.now();
            List<PasswordResetTokenEntity> activeTokens = passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
            if (!activeTokens.isEmpty()) {
                for (PasswordResetTokenEntity existing : activeTokens) {
                    existing.setUsedAt(now);
                }
                passwordResetTokenRepository.saveAll(activeTokens);
            }

            String rawToken = generateSecureToken();
            PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
            tokenEntity.setId("prt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            tokenEntity.setUserId(user.getId());
            tokenEntity.setTokenHash(sha256Hex(rawToken));
            tokenEntity.setExpiresAt(now.plus(passwordResetMailService.getExpirationMinutes(), ChronoUnit.MINUTES));
            tokenEntity.setCreatedAt(now);
            passwordResetTokenRepository.save(tokenEntity);

            passwordResetMailService.sendResetEmail(user.getEmail(), user.getDisplayName(), rawToken);
        }

        return new ForgotPasswordResponse(
                "If an account exists for that email, a password reset link has been sent.",
                smtpConfigured
        );
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String rawToken = request.token().trim();
        String tokenHash = sha256Hex(rawToken);

        PasswordResetTokenEntity tokenEntity = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or has expired"));

        Instant now = Instant.now();
        if (tokenEntity.getUsedAt() != null || !tokenEntity.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or has expired");
        }

        UserEntity user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or has expired"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }
        FeatureFlagsDto flags = featureFlagsService.getFlags();
        if (Boolean.TRUE.equals(flags.maintenanceMode()) && !"SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is currently under maintenance");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(now);
        userRepository.save(user);

        List<PasswordResetTokenEntity> activeTokens = passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        for (PasswordResetTokenEntity t : activeTokens) {
            t.setUsedAt(now);
        }
        passwordResetTokenRepository.saveAll(activeTokens);

        return Map.of("message", "Password has been reset successfully");
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private String validateAndNormalizeUsername(String rawUsername) {
        String username = rawUsername == null ? "" : rawUsername.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username must be 3-32 characters of lowercase letters, digits, '-' or '_'"
            );
        }
        if (RESERVED_USERNAMES.contains(username.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This username is reserved");
        }
        return username;
    }

    private String generateUniqueUsername(String preferredUsername, String email) {
        String raw = (preferredUsername != null && !preferredUsername.isBlank())
                ? preferredUsername
                : (email.contains("@") ? email.substring(0, email.indexOf('@')) : email);

        String base = raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^[-_]+|[-_]+$", "");

        if (base.isEmpty()) {
            base = "user";
        } else if (base.length() < 3) {
            base = base + "-user";
        }
        if (base.length() > 32) {
            base = base.substring(0, 32).replaceAll("[-_]+$", "");
            if (base.length() < 3) {
                base = "user";
            }
        }

        String candidate = base;
        int suffix = 2;
        while (RESERVED_USERNAMES.contains(candidate) || userRepository.existsByUsernameIgnoreCase(candidate)) {
            String suffixStr = "-" + suffix++;
            int maxBaseLen = Math.max(1, 32 - suffixStr.length());
            String prefix = base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base;
            candidate = prefix + suffixStr;
        }
        return candidate;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private UserProfileDto toProfileDto(UserEntity user) {
        long usedPages = pageRepository.countByOwnerId(user.getId());
        long usedStorage = uploadedFileRepository.sumSizeBytesByOwnerId(user.getId());
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        boolean googleLinked = user.getGoogleSub() != null && !user.getGoogleSub().isBlank();
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                hasPassword,
                googleLinked,
                user.getRole(),
                user.getStatus(),
                user.getMaxPages(),
                user.getMaxStorageBytes(),
                usedPages,
                usedStorage
        );
    }
}
