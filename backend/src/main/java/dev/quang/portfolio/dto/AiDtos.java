package dev.quang.portfolio.dto;

import java.util.List;
import java.util.Map;

public class AiDtos {

    public record AdminAiConfigDto(
            boolean enabled,
            String baseUrl,
            String model,
            boolean hasApiKey,
            String apiKeyMasked
    ) {}

    public record UpdateAiConfigRequest(
            Boolean enabled,
            String baseUrl,
            String apiKey,
            Boolean clearApiKey,
            String model
    ) {}

    public record TestAiConfigRequest(
            String baseUrl,
            String apiKey
    ) {}

    public record TestAiConfigResponse(
            boolean ok,
            String message,
            List<String> models
    ) {}

    public record AiStatusDto(
            boolean available
    ) {}

    public record ParseCvRequest(
            String sourceType,
            String fileName,
            List<String> pageImages,
            String rawText,
            Map<String, Object> colorHints
    ) {}

    public record ParseCvResponse(
            Map<String, Object> theme,
            Map<String, Object> doc,
            List<String> notes
    ) {}

    public record TranslateDocRequest(
            Map<String, Object> doc,
            String targetLocale
    ) {}

    public record TranslateDocResponse(
            Map<String, Object> doc,
            int translatedCount
    ) {}

    public record AiChatMessageDto(
            String role,
            String content
    ) {}

    public record BuildPageRequest(
            String prompt,
            String mode,
            String pageType,
            String language,
            Map<String, Object> currentDoc,
            Map<String, Object> currentTheme,
            List<AiChatMessageDto> history
    ) {}

    public record BuildPageResponse(
            String action,
            String reply,
            Map<String, Object> theme,
            Map<String, Object> doc,
            List<String> notes
    ) {}
}
