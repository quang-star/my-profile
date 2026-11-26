package dev.quang.portfolio.service;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleTokenVerifierService {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final Set<String> VALID_ISSUERS = Set.of(
            "accounts.google.com",
            "https://accounts.google.com"
    );
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String clientId;

    @Autowired
    public GoogleTokenVerifierService(
            @Value("${app.google.client-id:}") String clientId
    ) {
        this(RestClient.create(), clientId);
    }

    GoogleTokenVerifierService(RestClient restClient, String clientId) {
        this.restClient = restClient;
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    public boolean isEnabled() {
        return !clientId.isBlank();
    }

    public String getClientId() {
        return clientId;
    }

    /**
     * Verifies a Google ID token via Google's tokeninfo endpoint and returns the verified claims.
     */
    public GoogleUserInfo verify(String idToken) {
        if (!isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google Sign-In is not configured");
        }
        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google credential");
        }

        URI uri = UriComponentsBuilder.fromUriString(TOKENINFO_URL)
                .queryParam("id_token", idToken.trim())
                .build()
                .toUri();

        Map<String, Object> claims;
        try {
            claims = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired Google token");
        }

        if (claims == null || claims.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token response");
        }

        String aud = stringClaim(claims, "aud");
        String iss = stringClaim(claims, "iss");
        String emailVerified = stringClaim(claims, "email_verified");
        String sub = stringClaim(claims, "sub");
        String email = stringClaim(claims, "email");

        if (!clientId.equals(aud)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token audience mismatch");
        }
        if (iss == null || !VALID_ISSUERS.contains(iss)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token issuer invalid");
        }
        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
        }
        if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account missing required claims");
        }

        return new GoogleUserInfo(
                sub.trim(),
                email.trim(),
                stringClaim(claims, "name"),
                stringClaim(claims, "picture")
        );
    }

    private static String stringClaim(Map<String, Object> claims, String key) {
        Object val = claims.get(key);
        return val == null ? null : String.valueOf(val);
    }

    public record GoogleUserInfo(
            String sub,
            String email,
            String name,
            String picture
    ) {}
}
