package dev.quang.portfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.quang.portfolio.dto.MultiTenantDtos.HomeConfigDto;
import dev.quang.portfolio.entity.SystemSettingEntity;
import dev.quang.portfolio.repository.SystemSettingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HomeConfigService {

    private static final String SETTING_KEY = "home.config.json";
    private static final String STATE_VISIBLE = "visible";
    private static final String STATE_COMING_SOON = "coming_soon";
    private static final String STATE_HIDDEN = "hidden";
    private static final Set<String> VALID_STATES = Set.of(STATE_VISIBLE, STATE_COMING_SOON, STATE_HIDDEN);
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final SystemSettingRepository systemSettingRepository;
    private final ObjectMapper objectMapper;

    public HomeConfigService(SystemSettingRepository systemSettingRepository, ObjectMapper objectMapper) {
        this.systemSettingRepository = systemSettingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public HomeConfigDto getConfig() {
        return systemSettingRepository.findById(SETTING_KEY)
                .map(SystemSettingEntity::getSettingValue)
                .map(this::parseConfigJson)
                .orElseGet(HomeConfigService::defaultConfig);
    }

    @Transactional
    public HomeConfigDto updateConfig(HomeConfigDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Home config payload is required");
        }
        HomeConfigDto current = getConfig();
        HomeConfigDto next = new HomeConfigDto(
                validateOrKeep(request.heroPreview(), current.heroPreview(), "heroPreview"),
                validateOrKeep(request.featured(), current.featured(), "featured"),
                validateOrKeep(request.features(), current.features(), "features"),
                validateOrKeep(request.templates(), current.templates(), "templates"),
                validateOrKeep(request.howItWorks(), current.howItWorks(), "howItWorks"),
                validateOrKeep(request.bottomCta(), current.bottomCta(), "bottomCta")
        );

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("heroPreview", next.heroPreview());
        payload.put("featured", next.featured());
        payload.put("features", next.features());
        payload.put("templates", next.templates());
        payload.put("howItWorks", next.howItWorks());
        payload.put("bottomCta", next.bottomCta());

        try {
            String json = objectMapper.writeValueAsString(payload);
            SystemSettingEntity entity = systemSettingRepository.findById(SETTING_KEY)
                    .orElseGet(SystemSettingEntity::new);
            entity.setSettingKey(SETTING_KEY);
            entity.setSettingValue(json);
            entity.setUpdatedAt(Instant.now());
            systemSettingRepository.save(entity);
            return next;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist home config", ex);
        }
    }

    private HomeConfigDto parseConfigJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultConfig();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(raw, MAP_TYPE);
            HomeConfigDto defaults = defaultConfig();
            return new HomeConfigDto(
                    normalizeOrDefault(parsed.get("heroPreview"), defaults.heroPreview()),
                    normalizeOrDefault(parsed.get("featured"), defaults.featured()),
                    normalizeOrDefault(parsed.get("features"), defaults.features()),
                    normalizeOrDefault(parsed.get("templates"), defaults.templates()),
                    normalizeOrDefault(parsed.get("howItWorks"), defaults.howItWorks()),
                    normalizeOrDefault(parsed.get("bottomCta"), defaults.bottomCta())
            );
        } catch (Exception ignored) {
            return defaultConfig();
        }
    }

    private static String validateOrKeep(String incoming, String fallback, String fieldName) {
        if (incoming == null || incoming.isBlank()) {
            return fallback;
        }
        String normalized = incoming.trim().toLowerCase(Locale.ROOT);
        if (!VALID_STATES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid state for " + fieldName + ": " + incoming + " (expected visible, coming_soon, or hidden)"
            );
        }
        return normalized;
    }

    private static String normalizeOrDefault(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return VALID_STATES.contains(normalized) ? normalized : fallback;
    }

    private static HomeConfigDto defaultConfig() {
        return new HomeConfigDto(
                STATE_COMING_SOON,
                STATE_COMING_SOON,
                STATE_COMING_SOON,
                STATE_COMING_SOON,
                STATE_COMING_SOON,
                STATE_HIDDEN
        );
    }
}
