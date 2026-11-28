package dev.quang.portfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.quang.portfolio.dto.MultiTenantDtos.FeatureFlagsDto;
import dev.quang.portfolio.entity.SystemSettingEntity;
import dev.quang.portfolio.repository.SystemSettingRepository;
import dev.quang.portfolio.security.AuthenticatedUser;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeatureFlagsService {

    private static final String SETTING_KEY = "feature.flags.json";
    private static final int MAX_MAINTENANCE_MSG_LEN = 280;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final SystemSettingRepository systemSettingRepository;
    private final ObjectMapper objectMapper;

    public FeatureFlagsService(SystemSettingRepository systemSettingRepository, ObjectMapper objectMapper) {
        this.systemSettingRepository = systemSettingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public FeatureFlagsDto getFlags() {
        return systemSettingRepository.findById(SETTING_KEY)
                .map(SystemSettingEntity::getSettingValue)
                .map(this::parseFlagsJson)
                .orElseGet(FeatureFlagsService::defaultFlags);
    }

    @Transactional
    public FeatureFlagsDto updateFlags(FeatureFlagsDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feature flags payload is required");
        }
        FeatureFlagsDto current = getFlags();
        String msg = request.maintenanceMessage() != null
                ? sanitizeMessage(request.maintenanceMessage())
                : current.maintenanceMessage();

        FeatureFlagsDto next = new FeatureFlagsDto(
                coalesceBool(request.registrationEnabled(), current.registrationEnabled()),
                coalesceBool(request.googleLoginEnabled(), current.googleLoginEnabled()),
                coalesceBool(request.aiCopilotEnabled(), current.aiCopilotEnabled()),
                coalesceBool(request.aiTranslateEnabled(), current.aiTranslateEnabled()),
                coalesceBool(request.cvImportEnabled(), current.cvImportEnabled()),
                coalesceBool(request.pdfExportEnabled(), current.pdfExportEnabled()),
                coalesceBool(request.mediaUploadEnabled(), current.mediaUploadEnabled()),
                coalesceBool(request.publicTemplatesEnabled(), current.publicTemplatesEnabled()),
                coalesceBool(request.publicContactEnabled(), current.publicContactEnabled()),
                coalesceBool(request.maintenanceMode(), current.maintenanceMode()),
                msg
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("registrationEnabled", next.registrationEnabled());
        payload.put("googleLoginEnabled", next.googleLoginEnabled());
        payload.put("aiCopilotEnabled", next.aiCopilotEnabled());
        payload.put("aiTranslateEnabled", next.aiTranslateEnabled());
        payload.put("cvImportEnabled", next.cvImportEnabled());
        payload.put("pdfExportEnabled", next.pdfExportEnabled());
        payload.put("mediaUploadEnabled", next.mediaUploadEnabled());
        payload.put("publicTemplatesEnabled", next.publicTemplatesEnabled());
        payload.put("publicContactEnabled", next.publicContactEnabled());
        payload.put("maintenanceMode", next.maintenanceMode());
        payload.put("maintenanceMessage", next.maintenanceMessage());

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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist feature flags", ex);
        }
    }

    public boolean isEnabledFor(Boolean flagEnabled, AuthenticatedUser user) {
        return Boolean.TRUE.equals(flagEnabled) || (user != null && user.isSuperAdmin());
    }

    public void requireFeature(Boolean flagEnabled, AuthenticatedUser user, String featureLabel) {
        if (!isEnabledFor(flagEnabled, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, featureLabel + " is temporarily disabled");
        }
    }

    public void requireNotInMaintenance(AuthenticatedUser user) {
        if (Boolean.TRUE.equals(getFlags().maintenanceMode()) && (user == null || !user.isSuperAdmin())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System is currently under maintenance");
        }
    }

    private FeatureFlagsDto parseFlagsJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultFlags();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(raw, MAP_TYPE);
            FeatureFlagsDto defaults = defaultFlags();
            return new FeatureFlagsDto(
                    boolOrDefault(parsed.get("registrationEnabled"), defaults.registrationEnabled()),
                    boolOrDefault(parsed.get("googleLoginEnabled"), defaults.googleLoginEnabled()),
                    boolOrDefault(parsed.get("aiCopilotEnabled"), defaults.aiCopilotEnabled()),
                    boolOrDefault(parsed.get("aiTranslateEnabled"), defaults.aiTranslateEnabled()),
                    boolOrDefault(parsed.get("cvImportEnabled"), defaults.cvImportEnabled()),
                    boolOrDefault(parsed.get("pdfExportEnabled"), defaults.pdfExportEnabled()),
                    boolOrDefault(parsed.get("mediaUploadEnabled"), defaults.mediaUploadEnabled()),
                    boolOrDefault(parsed.get("publicTemplatesEnabled"), defaults.publicTemplatesEnabled()),
                    boolOrDefault(parsed.get("publicContactEnabled"), defaults.publicContactEnabled()),
                    boolOrDefault(parsed.get("maintenanceMode"), defaults.maintenanceMode()),
                    strOrDefault(parsed.get("maintenanceMessage"), defaults.maintenanceMessage())
            );
        } catch (Exception ignored) {
            return defaultFlags();
        }
    }

    private static Boolean coalesceBool(Boolean incoming, Boolean fallback) {
        return incoming != null ? incoming : fallback;
    }

    private static Boolean boolOrDefault(Object raw, Boolean fallback) {
        if (raw instanceof Boolean b) return b;
        if (raw instanceof String s) {
            if ("true".equalsIgnoreCase(s.trim())) return true;
            if ("false".equalsIgnoreCase(s.trim())) return false;
        }
        return fallback;
    }

    private static String strOrDefault(Object raw, String fallback) {
        if (raw instanceof String s) return sanitizeMessage(s);
        return fallback;
    }

    private static String sanitizeMessage(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        return trimmed.length() > MAX_MAINTENANCE_MSG_LEN
                ? trimmed.substring(0, MAX_MAINTENANCE_MSG_LEN)
                : trimmed;
    }

    private static FeatureFlagsDto defaultFlags() {
        return new FeatureFlagsDto(
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                ""
        );
    }
}
