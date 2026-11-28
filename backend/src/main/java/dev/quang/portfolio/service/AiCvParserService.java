package dev.quang.portfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.quang.portfolio.dto.AiDtos.*;
import dev.quang.portfolio.entity.SystemSettingEntity;
import dev.quang.portfolio.repository.SystemSettingRepository;
import dev.quang.portfolio.security.SecretCryptoService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiCvParserService {

    private static final Logger log = LoggerFactory.getLogger(AiCvParserService.class);

    private static final String KEY_ENABLED = "ai.enabled";
    private static final String KEY_BASE_URL = "ai.base_url";
    private static final String KEY_API_KEY = "ai.api_key";
    private static final String KEY_MODEL = "ai.model";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final Pattern NON_TRANSLATABLE_TOKEN_PATTERN = Pattern.compile(
            "^(?:[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}|(?:https?://|mailto:|tel:|github\\.com/|linkedin\\.com/)\\S+|[\\d\\s+\\-().,:/|·•✉📞🎂%]+)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final String TRANSLATE_SYSTEM_PROMPT = """
            You are a bilingual Vietnamese <-> English CV, Portfolio, and Bio translator.
            You will receive a JSON array of strings extracted from a structured rich-text document.
            Translate each string naturally and idiomatically into the requested target language while keeping EXACT array length and order.

            RULES:
            - Return ONLY a valid JSON array of strings (same length as input, no markdown fences, no commentary).
            - Keep person names (e.g. NGUYEN KIM QUANG), company/university proper names (unless commonly localized or standard), emails, URLs, handles, dates/numbers, and technical stack keywords (Java, Spring Boot, React, Vue.js, FastAPI, PostgreSQL, Docker, AWS, Coolify, OOP, RESTful API, etc.) accurate and intact.
            - Translate section headings (e.g. SUMMARY <-> MỤC TIÊU NGHỀ NGHIỆP, EDUCATION <-> HỌC VẤN, SKILLS <-> KỸ NĂNG, PROJECTS <-> DỰ ÁN, EXPERIENCE <-> KINH NGHIỆM LÀM VIỆC) and descriptions naturally.
            - Preserve leading/trailing spaces or punctuation separators if present in a segment.
            """;

    private static final String BUILD_PAGE_SYSTEM_PROMPT = """
            You are an expert CV, Portfolio, and Bio Page Copilot & Architect.
            You receive the user's conversation history plus the current page state (Theme + Tiptap Document JSON) and the user's latest message.
            Decide intelligently whether the user is asking for advice/feedback/Q&A OR requesting an actual edit/generation of the page, and ALWAYS respond with ONLY a valid JSON object (no markdown fences, no commentary outside the JSON).

            MODE A — ADVICE / FEEDBACK / Q&A (do NOT modify the page):
            When the user asks questions ("CV này ổn chưa?", "Nên sửa gì?", "Viết summary sao cho hay?"), asks for opinions/critique, compares options, or explicitly says only to advise without editing yet:
            Return ONLY:
            {
              "action": "chat",
              "reply": "Clear, helpful, actionable answer in the user's language (Vietnamese or English). Do NOT include doc or theme."
            }

            MODE B — CREATE NEW PAGE OR APPLY EDITS TO CURRENT PAGE:
            When the user asks you to build a page from scratch, apply a previously discussed suggestion ("Làm theo ý số 2 đi", "Sửa giúp mình..."), rewrite text on the page, change colors/fonts/spacing, or restructure columns/sections:
            Return ONLY:
            {
              "action": "apply",
              "reply": "1-2 concise sentences summarizing what you created or updated.",
              "theme": {
                "pageLayout": "a4" | "web" | "narrow",
                "sheetMode": "paper" | "transparent",
                "bgColor": "#hex",
                "paperBgColor": "#hex",
                "primaryColor": "#hex",
                "textColor": "#hex",
                "fontFamily": "inter" | "be-vietnam" | "roboto" | "merriweather" | "lora" | "times" | "arial" | "jetbrains",
                "fontScalePct": 96,
                "lineHeightValue": 1.45,
                "paragraphGapPx": 4,
                "marginTop": 28,
                "marginBottom": 28,
                "marginLeft": 28,
                "marginRight": 28
              },
              "doc": {
                "type": "doc",
                "content": [ ... ]
              }
            }

            Allowed Tiptap block nodes when action="apply":
            1) "boxBlock": { "type": "boxBlock", "attrs": { "bgColor": "#hex or transparent", "textColor": "#hex or ''", "borderColor": "#hex or transparent", "borderWidth": 0..4, "borderSide": "all"|"left"|"bottom"|"top", "radius": 0..24, "shadow": "none"|"sm"|"md", "paddingX": 0..32, "paddingY": 0..28, "marginTop": 0, "marginBottom": 12, "widthPct": 100, "align": "left" }, "content": [...] }
            2) "columnsBlock" (2 to 6 columns, widths summing to 100): { "type": "columnsBlock", "attrs": { "widths": [34, 66], "gap": 18, "marginTop": 0, "marginBottom": 14, "stackMobile": true }, "content": [ { "type": "columnCell", "attrs": { "bgColor": "#hex or transparent", "textColor": "#hex or ''", "borderColor": "#hex or transparent", "paddingX": 12, "paddingY": 10, "radius": 8 }, "content": [...] }, ... ] }
            3) "heading": { "type": "heading", "attrs": { "level": 1|2|3, "textAlign": "left"|"center"|"right" }, "content": [ text nodes ] }
            4) "paragraph": { "type": "paragraph", "attrs": { "textAlign": "left"|"center"|"right"|"justify" }, "content": [ text nodes ] }
            5) "bulletList" / "orderedList": { "type": "bulletList", "content": [ { "type": "listItem", "content": [ { "type": "paragraph", "content": [...] } ] } ] }
            6) "horizontalRule": { "type": "horizontalRule", "attrs": { "color": "#hex", "thickness": 1, "lineStyle": "solid"|"dashed"|"dotted", "widthPct": 100, "align": "left"|"center"|"right", "marginTop": 2, "marginBottom": 6 } }
            7) "skillBarBlock": { "type": "skillBarBlock", "attrs": { "label": "Skill", "level": 85, "color": "#hex", "showPercent": true, "marginBottom": 8 } }
            8) "badgeListBlock": { "type": "badgeListBlock", "attrs": { "items": ["React", "Java"], "bgColor": "#hex", "textColor": "#hex", "borderColor": "#hex", "radius": 999, "align": "left", "marginBottom": 10 } }
            9) "linkButtonBlock": { "type": "linkButtonBlock", "attrs": { "items": [{"label": "Email", "href": "mailto:...", "variant": "primary"|"outline"}], "align": "left", "marginBottom": 10 } }
            10) "table": { "type": "table", "attrs": { "borderStyle": "none"|"horizontal"|"grid", "borderColor": "", "cellPadding": "compact"|"normal"|"relaxed", "striped": false }, "content": [ { "type": "tableRow", "content": [ { "type": "tableCell", "content": [ { "type": "paragraph", ... } ] } ] } ] }

            Text nodes & marks:
            { "type": "text", "text": "...", "marks": [ { "type": "bold" }, { "type": "italic" }, { "type": "underline" }, { "type": "link", "attrs": { "href": "https://..." } }, { "type": "textStyle", "attrs": { "color": "#hex", "fontSize": "14px" } } ] }

            RULES:
            - Always reply in the same language the user is using unless they ask otherwise.
            - Never return "doc" when action="chat" — keep advice fast and concise.
            - When editing an existing page (action="apply"), preserve ALL real personal details, links, dates, companies, projects, and schools unless explicitly asked to replace/remove them.
            - Wrap each logical section inside ONE clean outer "boxBlock" (heading + horizontalRule if underlined + body/table/lists — never nested bottom-bordered heading boxes).
            - Never emit empty text nodes (text: ""). Every boxBlock, columnCell, and listItem must have >= 1 child block.
            """;

    private static final String SYSTEM_PROMPT = """
            You are an expert CV/Portfolio Visual Designer and Document Structure Parser.
            Analyze the user's CV (from the rendered page images and/or extracted text/HTML and color hints)
            and reproduce its visual layout, color palette, typography sizes, column proportions, and full text content
            into a valid JSON object matching our Tiptap Document + PageTheme schema.

            Return ONLY a valid JSON object (no markdown fences, no commentary) with exact top-level keys:
            {
              "theme": {
                "pageLayout": "a4" | "web",
                "sheetMode": "paper" | "transparent",
                "bgColor": "#hex",
                "paperBgColor": "#hex",
                "primaryColor": "#hex",
                "textColor": "#hex",
                "fontFamily": "inter" | "be-vietnam" | "roboto" | "merriweather" | "lora" | "times" | "arial" | "jetbrains",
                "fontScalePct": 96,
                "lineHeightValue": 1.4,
                "paragraphGapPx": 4,
                "marginTop": 28,
                "marginBottom": 28,
                "marginLeft": 28,
                "marginRight": 28
              },
              "doc": {
                "type": "doc",
                "content": [ ... ]
              }
            }

            Allowed Tiptap block nodes inside "content":
            1) "boxBlock" (container for banners, headers, or cards):
               {
                 "type": "boxBlock",
                 "attrs": {
                   "bgColor": "#hex or transparent",
                   "textColor": "#hex or ''",
                   "borderColor": "#hex or transparent",
                   "borderWidth": 0..4,
                   "borderSide": "all" | "left" | "bottom" | "top",
                   "radius": 0..24,
                   "shadow": "none" | "sm" | "md",
                   "paddingX": 0..32,
                   "paddingY": 0..28,
                   "marginTop": 0,
                   "marginBottom": 14,
                   "widthPct": 100,
                   "align": "left"
                 },
                 "content": [ ...block nodes... ]
               }
            2) "columnsBlock" (2 to 6 columns layout — use any column count and percentage widths summing to 100 matching the visual layout):
               {
                 "type": "columnsBlock",
                 "attrs": { "widths": [34, 66], "gap": 18, "marginTop": 0, "marginBottom": 14, "stackMobile": true },
                 "content": [
                   {
                     "type": "columnCell",
                     "attrs": { "bgColor": "#hex or transparent", "textColor": "#hex or ''", "borderColor": "transparent", "paddingX": 12, "paddingY": 10, "radius": 8 },
                     "content": [ ...block nodes... ]
                   }, ...
                 ]
               }
            3) "heading": { "type": "heading", "attrs": { "level": 1|2|3, "textAlign": "left"|"center"|"right" }, "content": [ text nodes ] }
            4) "paragraph": { "type": "paragraph", "attrs": { "textAlign": "left"|"center"|"right"|"justify" }, "content": [ text nodes ] }
            5) "bulletList" / "orderedList": { "type": "bulletList", "content": [ { "type": "listItem", "content": [ { "type": "paragraph", "content": [...] } ] } ] }
            6) "horizontalRule" (directly editable horizontal divider line — ALWAYS prefer this for section heading underlines and visual separators!): { "type": "horizontalRule", "attrs": { "color": "#111827", "thickness": 1, "lineStyle": "solid"|"dashed"|"dotted", "widthPct": 100, "align": "left"|"center"|"right", "marginTop": 2, "marginBottom": 6 } }
            7) "skillBarBlock": { "type": "skillBarBlock", "attrs": { "label": "Skill", "level": 85, "color": "#hex", "showPercent": true, "marginBottom": 8 } }
            8) "badgeListBlock": { "type": "badgeListBlock", "attrs": { "items": ["React", "Java"], "bgColor": "#hex", "textColor": "#hex", "borderColor": "#hex", "radius": 999, "align": "left", "marginBottom": 10 } }
            9) "linkButtonBlock": { "type": "linkButtonBlock", "attrs": { "items": [{"label": "Email", "href": "mailto:...", "variant": "primary"|"outline"}], "align": "left", "marginBottom": 10 } }
            10) "table" (use ONLY for multi-row structured grids such as a 2-column Skills matrix "Category | Items" — NEVER use "table" for a single-row Title/Date header!): { "type": "table", "content": [ { "type": "tableRow", "content": [ { "type": "tableCell", "content": [ { "type": "paragraph", ... } ] } ] } ] }
               For Title/Institution/Project on left + Date on right, use a borderless 2-column "columnsBlock":
               { "type": "columnsBlock", "attrs": { "widths": [74, 26], "gap": 8, "marginTop": 4, "marginBottom": 2, "stackMobile": false }, "content": [
                 { "type": "columnCell", "attrs": { "bgColor": "transparent", "textColor": "", "borderColor": "transparent", "paddingX": 0, "paddingY": 0, "radius": 0 }, "content": [ { "type": "paragraph", "attrs": { "textAlign": "left" }, "content": [ ... ] } ] },
                 { "type": "columnCell", "attrs": { "bgColor": "transparent", "textColor": "", "borderColor": "transparent", "paddingX": 0, "paddingY": 0, "radius": 0 }, "content": [ { "type": "paragraph", "attrs": { "textAlign": "right" }, "content": [ ... ] } ] }
               ] }

            Text nodes & Marks:
            { "type": "text", "text": "...", "marks": [
              { "type": "bold" },
              { "type": "italic" },
              { "type": "underline" },
              { "type": "link", "attrs": { "href": "https://..." } },
              { "type": "textStyle", "attrs": { "color": "#hex", "fontSize": "14px" } }
            ] }

            CRITICAL RULES:
            - Preserve ALL text in its original language (e.g. Vietnamese diacritics or English). Do NOT invent, translate, or summarize away real details.
            - SECTION GROUPING (CRITICAL): Wrap EVERY major logical section (e.g. SUMMARY, EDUCATION, SKILLS, EXPERIENCE, PROJECTS, CERTIFICATIONS, AWARDS, ACTIVITIES, REFERENCES, INTERESTS) inside ONE outer "boxBlock" ("bgColor": "transparent", "textColor": "", "borderColor": "transparent", "borderWidth": 0, "paddingX": 0, "paddingY": 4, "marginTop": 0, "marginBottom": 12, "widthPct": 100) containing BOTH the section heading/underline AND ALL items, tables, bullets, or inner columnsBlock belonging to that section! NEVER leave a section's table, job entries, or bullet lists outside the section's boxBlock as loose siblings — the user must be able to hover or drag an entire section (heading + all its content) as a single unit!
            - LAYOUT FIDELITY: Classify the visual layout directly from the image without assuming a fixed template:
              1) Single-column vertical flow -> stack section boxBlocks top-to-bottom at full width.
              2) Full-height sidebar + main column -> use one outer columnsBlock (left or right sidebar width estimated visually, e.g. [32, 68] or [66, 34]) containing each sidebar section inside the sidebar columnCell and each main section inside the main columnCell (optionally preceded by a full-width top header banner if the header spans across the top).
              3) Multi-band mixed layout -> reproduce each distinct horizontal band from top to bottom with its own column count (1, 2, 3, or 4 columns) and visually estimated integer widths summing to 100. Never force a sidebar when the image is single-column or multi-band, and never break a continuous full-height sidebar into separate horizontal bands.
            - NO FAKE SIDEBAR BACKGROUND: Only give a columnCell a shaded/colored bgColor (like #f3f4f6 or #f8fafc) if the original CV image visibly has a shaded sidebar box. If the CV background is white throughout, keep columnCell bgColor: "transparent" and borderColor: "transparent".
            - TIMELINE LINES, AVATARS & WATERMARKS: If an experience/activity entry has a vertical timeline line under the date/company, wrap the left cell's content or use a boxBlock with borderSide: "left", borderWidth: 2, borderColor: "#d4d4d8" (or the CV's line color), paddingX: 12, paddingY: 2, bgColor: "transparent". Never output literal "[Avatar Placeholder]" text or platform watermarks like "© topcv.vn" — if the CV shows an avatar placeholder on top-left, use a clean shaded boxBlock (bgColor: "#f4f4f5", radius: 8, paddingY: 44) with a subtle centered label "Ảnh đại diện".
            - COLOR FIDELITY: Always trust the rendered CV page image! If the CV is black-and-white / monochrome (or sampled color hints say "isMonochrome": true with empty "palette"), set theme "primaryColor": "#111827", "textColor": "#111827", "bgColor": "#ffffff", "paperBgColor": "#ffffff", and keep ALL headings, links, dividers, and text black/dark-gray. NEVER invent blue (#1d4ed8) or colored banners/badges that do not exist in the original CV image.
            - FONT FIDELITY: Match serif vs sans-serif from the CV image and "detectedFont" in sampled color hints. For classic, academic, LaTeX, or Times New Roman serif CVs, set "fontFamily": "times" (or "lora" / "merriweather"); for Arial/Helvetica CVs use "arial"; only use "inter", "be-vietnam", or "roboto" for modern sans-serif CVs.
            - SPACING, MARGINS & TYPOGRAPHY FIDELITY: Visually estimate the outer page margins (16-24px for tight dense CVs, 28-36px for standard pages, 40-56px for spacious editorial pages), lineHeightValue (1.2-1.4 for dense, 1.45-1.65 for normal, 1.7-1.9 for airy), paragraphGapPx (2-4 for tight, 5-8 for normal), and fontScalePct (88-96 for compact 1-page CVs, 100 for standard). For ANY section heading that has a horizontal underline beneath it, ALWAYS place the "heading" node directly inside the section's single outer "boxBlock" followed immediately by a "horizontalRule" node matching the CV's line color and thickness (1 or 2). NEVER wrap a heading in a second nested bottom-bordered "boxBlock", NEVER put borderSide: "bottom" on the outer section "boxBlock", and NEVER create an empty "boxBlock" containing a blank paragraph just to draw a line — each section must stay a clean 1-level "boxBlock" so the user can edit or delete the divider directly! Do NOT wrap clean pages in unnecessary cards or colored banners.
            - TYPOGRAPHY HIERARCHY: Omit redundant fontSize on ordinary body paragraphs so they inherit the document base scale cleanly; only set textStyle fontSize on distinct hierarchy levels (e.g. 24px-28px for Name, 13px-16px for Subtitle/Section headings, 11px-12px for small dates/captions).
            - Ensure every boxBlock, columnCell, and listItem has at least one child block (e.g. a paragraph).
            - Do NOT include empty text nodes (text: "").
            """;

    private final SystemSettingRepository settingRepository;
    private final SecretCryptoService secretCryptoService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean defaultEnabled;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultModel;
    private final boolean rewriteLocalhostInDocker;

    public AiCvParserService(
            SystemSettingRepository settingRepository,
            SecretCryptoService secretCryptoService,
            ObjectMapper objectMapper,
            @Value("${app.ai.enabled:true}") boolean defaultEnabled,
            @Value("${app.ai.base-url:http://host.docker.internal:20128/v1}") String defaultBaseUrl,
            @Value("${app.ai.api-key:}") String defaultApiKey,
            @Value("${app.ai.model:kr/claude-sonnet-4.5}") String defaultModel,
            @Value("${app.ai.rewrite-localhost-in-docker:true}") boolean rewriteLocalhostInDocker
    ) {
        this.settingRepository = settingRepository;
        this.secretCryptoService = secretCryptoService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.defaultEnabled = defaultEnabled;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultApiKey = defaultApiKey;
        this.defaultModel = defaultModel;
        this.rewriteLocalhostInDocker = rewriteLocalhostInDocker;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migratePlaintextApiKeyIfNeeded() {
        settingRepository.findById(KEY_API_KEY).ifPresent(entity -> {
            String raw = entity.getSettingValue();
            if (raw != null && !raw.isBlank() && !secretCryptoService.isEncrypted(raw.trim())) {
                entity.setSettingValue(secretCryptoService.encrypt(raw.trim()));
                entity.setUpdatedAt(Instant.now());
                settingRepository.save(entity);
            }
        });
    }

    @Transactional(readOnly = true)
    public AdminAiConfigDto getAdminConfig() {
        boolean enabled = readBoolean(KEY_ENABLED, defaultEnabled);
        String baseUrl = readString(KEY_BASE_URL, defaultBaseUrl);
        String model = readString(KEY_MODEL, defaultModel);
        String apiKey = readApiKey();
        boolean hasKey = apiKey != null && !apiKey.isBlank();
        return new AdminAiConfigDto(enabled, baseUrl, model, hasKey, maskKey(apiKey));
    }

    @Transactional
    public AdminAiConfigDto updateAdminConfig(UpdateAiConfigRequest request) {
        if (request.enabled() != null) {
            saveSetting(KEY_ENABLED, String.valueOf(request.enabled()));
        }
        String currentSavedBase = readString(KEY_BASE_URL, defaultBaseUrl);
        boolean supplyingNewKey = request.apiKey() != null && !request.apiKey().isBlank();
        boolean clearingKey = Boolean.TRUE.equals(request.clearApiKey());
        if (request.baseUrl() != null) {
            String trimmed = request.baseUrl().trim();
            if (trimmed.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Router Base URL cannot be empty");
            }
            if (!trimmed.matches("(?i)^https?://.+")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base URL must start with http:// or https://");
            }
            boolean baseChanged = !resolveEffectiveBaseUrl(trimmed)
                    .equalsIgnoreCase(resolveEffectiveBaseUrl(currentSavedBase));
            if (baseChanged && !supplyingNewKey && !clearingKey && !readApiKey().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Khi đổi sang Router Base URL khác, vui lòng nhập lại API Key mới (hoặc xóa key cũ) trước khi lưu."
                );
            }
            saveSetting(KEY_BASE_URL, stripTrailingSlash(trimmed));
        }
        if (clearingKey) {
            saveSetting(KEY_API_KEY, "");
        } else if (supplyingNewKey) {
            saveSetting(KEY_API_KEY, secretCryptoService.encrypt(request.apiKey().trim()));
        }
        if (request.model() != null && !request.model().isBlank()) {
            saveSetting(KEY_MODEL, request.model().trim());
        }
        return getAdminConfig();
    }

    @Transactional(readOnly = true)
    public AiStatusDto getPublicStatus() {
        boolean enabled = readBoolean(KEY_ENABLED, defaultEnabled);
        String baseUrl = readString(KEY_BASE_URL, defaultBaseUrl);
        String apiKey = readApiKey();
        boolean available = enabled && baseUrl != null && !baseUrl.isBlank() && apiKey != null && !apiKey.isBlank();
        return new AiStatusDto(available);
    }

    public TestAiConfigResponse testAndFetchModels(TestAiConfigRequest request) {
        String currentSavedBase = readString(KEY_BASE_URL, defaultBaseUrl);
        String rawBase = request != null && request.baseUrl() != null && !request.baseUrl().isBlank()
                ? request.baseUrl().trim()
                : currentSavedBase;
        boolean hasExplicitKey = request != null && request.apiKey() != null && !request.apiKey().isBlank();
        boolean baseChanged = !resolveEffectiveBaseUrl(rawBase)
                .equalsIgnoreCase(resolveEffectiveBaseUrl(currentSavedBase));
        if (!hasExplicitKey && baseChanged && !readApiKey().isBlank()) {
            return new TestAiConfigResponse(
                    false,
                    "Bạn vừa đổi Router Base URL — vì lý do bảo mật, hãy nhập lại API Key cho URL mới trước khi kiểm tra kết nối.",
                    List.of()
            );
        }
        String apiKey = hasExplicitKey
                ? request.apiKey().trim()
                : readApiKey();

        String effectiveBase = resolveEffectiveBaseUrl(rawBase);
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(effectiveBase + "/models"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET();
            if (apiKey != null && !apiKey.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return new TestAiConfigResponse(
                        false,
                        "Router trả về HTTP " + response.statusCode() + ": " + truncate(response.body(), 240),
                        List.of()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<String> models = new ArrayList<>();
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    String id = item.path("id").asText("").trim();
                    if (!id.isEmpty()) {
                        models.add(id);
                    }
                }
            }
            Collections.sort(models);
            return new TestAiConfigResponse(
                    true,
                    "Kết nối thành công tới " + effectiveBase + " (" + models.size() + " models)",
                    models
            );
        } catch (Exception ex) {
            return new TestAiConfigResponse(
                    false,
                    "Không kết nối được tới " + effectiveBase + ": " + ex.getMessage(),
                    List.of()
            );
        }
    }

    private record ActiveAiRouter(String effectiveBase, String apiKey, String model) {}

    private ActiveAiRouter requireConfiguredRouter(String featureLabel) {
        boolean enabled = readBoolean(KEY_ENABLED, defaultEnabled);
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tính năng " + featureLabel + " đang tắt trong Quản trị.");
        }
        String rawBase = readString(KEY_BASE_URL, defaultBaseUrl);
        String apiKey = readApiKey();
        String model = readString(KEY_MODEL, defaultModel);

        if (rawBase == null || rawBase.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chưa cấu hình AI Router hoặc API Key trong trang Quản trị hệ thống (/admin)."
            );
        }
        return new ActiveAiRouter(resolveEffectiveBaseUrl(rawBase), apiKey, model);
    }

    public ParseCvResponse parseCv(ParseCvRequest request) {
        ActiveAiRouter router = requireConfiguredRouter("AI phân tích CV");
        String effectiveBase = router.effectiveBase();
        String apiKey = router.apiKey();
        String model = router.model();

        List<String> images = request.pageImages() == null
                ? List.of()
                : request.pageImages().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> s.startsWith("data:image/"))
                        .limit(3)
                        .toList();

        String rawText = request.rawText() == null ? "" : truncate(request.rawText(), 30_000);
        if (images.isEmpty() && rawText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có dữ liệu ảnh hoặc nội dung văn bản để phân tích.");
        }

        List<String> notes = new ArrayList<>();
        try {
            Object userContent = buildParseCvUserContent(request, images, rawText, true);
            String assistantContent = sendChatCompletion(effectiveBase, apiKey, model, SYSTEM_PROMPT, userContent, 0.1, 8192);
            return buildParsedResponse(assistantContent, notes);
        } catch (RouterCallException ex) {
            if (!images.isEmpty() && !rawText.isBlank() && ex.statusCode >= 400 && ex.statusCode < 500) {
                notes.add("AI hiện tại không hỗ trợ ảnh đầu vào, đã tự chuyển sang phân tích bằng văn bản & tọa độ màu.");
                try {
                    Object fallbackUserContent = buildParseCvUserContent(request, List.of(), rawText, false);
                    String fallbackContent = sendChatCompletion(effectiveBase, apiKey, model, SYSTEM_PROMPT, fallbackUserContent, 0.1, 8192);
                    return buildParsedResponse(fallbackContent, notes);
                } catch (Exception retryEx) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "AI Router lỗi (" + ex.statusCode + "): " + truncate(describeException(retryEx), 280)
                    );
                }
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI Router lỗi (" + ex.statusCode + "): " + truncate(describeException(ex), 280)
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("AI CV parser call failed: {}", describeException(ex));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lỗi gọi AI Router: " + truncate(describeException(ex), 280));
        }
    }

    public TranslateDocResponse translateDoc(TranslateDocRequest request) {
        ActiveAiRouter router = requireConfiguredRouter("AI dịch tài liệu");
        if (request == null || request.doc() == null || ! (request.doc().get("content") instanceof List<?>)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài liệu nguồn không hợp lệ.");
        }
        String target = "en".equalsIgnoreCase(request.targetLocale()) ? "en" : "vi";

        try {
            Map<String, Object> clonedDoc = objectMapper.readValue(objectMapper.writeValueAsString(request.doc()), MAP_TYPE);
            List<String> originals = new ArrayList<>();
            List<Consumer<String>> setters = new ArrayList<>();
            collectTranslatableSlots(clonedDoc, originals, setters);

            if (originals.isEmpty()) {
                return new TranslateDocResponse(clonedDoc, 0);
            }

            String userPrompt = "Target language: " + ("en".equals(target) ? "English" : "Vietnamese")
                    + "\nJSON string array to translate (" + originals.size() + " items):\n"
                    + objectMapper.writeValueAsString(originals);

            String assistantOutput = sendChatCompletion(
                    router.effectiveBase(),
                    router.apiKey(),
                    router.model(),
                    TRANSLATE_SYSTEM_PROMPT,
                    userPrompt,
                    0.1,
                    8192
            );

            List<String> translated = objectMapper.readValue(extractJsonArray(assistantOutput), STRING_LIST_TYPE);
            int limit = Math.min(setters.size(), translated.size());
            int applied = 0;
            for (int i = 0; i < limit; i++) {
                String replacement = translated.get(i);
                if (replacement != null && (!replacement.isEmpty() || originals.get(i).isEmpty())) {
                    setters.get(i).accept(replacement);
                    applied++;
                }
            }
            return new TranslateDocResponse(clonedDoc, applied);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RouterCallException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI Router lỗi (" + ex.statusCode + "): " + truncate(ex.getMessage(), 260));
        } catch (Exception ex) {
            log.warn("AI translate-doc failed: {}", describeException(ex));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lỗi dịch bằng AI: " + truncate(describeException(ex), 260));
        }
    }

    @SuppressWarnings("unchecked")
    public BuildPageResponse buildPage(BuildPageRequest request) {
        ActiveAiRouter router = requireConfiguredRouter("Trợ lý AI");
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập câu hỏi hoặc yêu cầu cho AI.");
        }

        String language = "en".equalsIgnoreCase(request.language()) ? "English" : "Vietnamese";

        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", BUILD_PAGE_SYSTEM_PROMPT));

            if (request.history() != null && !request.history().isEmpty()) {
                int startIdx = Math.max(0, request.history().size() - 8);
                for (int i = startIdx; i < request.history().size(); i++) {
                    AiChatMessageDto msg = request.history().get(i);
                    if (msg == null || msg.content() == null || msg.content().isBlank()) continue;
                    String role = "assistant".equalsIgnoreCase(msg.role()) ? "assistant" : "user";
                    messages.add(Map.of("role", role, "content", truncate(msg.content().trim(), 1500)));
                }
            }

            StringBuilder latestUser = new StringBuilder();
            latestUser.append("Editor default UI language: ").append(language).append("\n");
            if (request.currentTheme() != null && !request.currentTheme().isEmpty()) {
                latestUser.append("Current page theme JSON:\n")
                        .append(objectMapper.writeValueAsString(request.currentTheme()))
                        .append("\n");
            }
            if (request.currentDoc() != null) {
                latestUser.append("Current page document JSON:\n")
                        .append(truncate(objectMapper.writeValueAsString(request.currentDoc()), 34_000))
                        .append("\n");
            }
            latestUser.append("\nUser message:\n").append(truncate(request.prompt().trim(), 6000));
            messages.add(Map.of("role", "user", "content", latestUser.toString()));

            String assistantOutput = sendChatCompletionMessages(
                    router.effectiveBase(),
                    router.apiKey(),
                    router.model(),
                    messages,
                    0.25,
                    8192
            );

            String cleaned = extractJsonObject(assistantOutput);
            try {
                Map<String, Object> parsed = objectMapper.readValue(cleaned, MAP_TYPE);
                String action = parsed.get("action") != null ? String.valueOf(parsed.get("action")).trim().toLowerCase(Locale.ROOT) : "";
                String reply = parsed.get("reply") instanceof String r ? r.trim() : "";

                if (("apply".equals(action) || (action.isEmpty() && parsed.get("doc") != null))
                        && parsed.get("doc") instanceof Map<?, ?> rawDoc
                        && "doc".equals(rawDoc.get("type"))
                        && rawDoc.get("content") instanceof List<?>) {
                    Map<String, Object> doc = (Map<String, Object>) rawDoc;
                    Map<String, Object> theme = parsed.get("theme") instanceof Map<?, ?> rawTheme
                            ? (Map<String, Object>) rawTheme
                            : Map.of();
                    String finalReply = reply.isEmpty() ? "Đã cập nhật trang theo yêu cầu." : reply;
                    return new BuildPageResponse("apply", finalReply, theme, doc, List.of());
                }

                if (!reply.isEmpty()) {
                    return new BuildPageResponse("chat", reply, Map.of(), null, List.of());
                }
            } catch (Exception ignored) {
                // Fall back to plain chat reply if model responded directly in prose
            }

            String fallbackReply = assistantOutput != null && !assistantOutput.isBlank()
                    ? assistantOutput.trim()
                    : "AI chưa có phản hồi rõ ràng, bạn thử hỏi lại cụ thể hơn nhé.";
            return new BuildPageResponse("chat", fallbackReply, Map.of(), null, List.of());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RouterCallException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI Router lỗi (" + ex.statusCode + "): " + truncate(ex.getMessage(), 260));
        } catch (Exception ex) {
            log.warn("AI build-page chat failed: {}", describeException(ex));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lỗi Trợ lý AI: " + truncate(describeException(ex), 260));
        }
    }

    @SuppressWarnings("unchecked")
    private void collectTranslatableSlots(Object node, List<String> originals, List<Consumer<String>> setters) {
        if (node instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Object typeObj = map.get("type");
            String type = typeObj != null ? typeObj.toString() : "";

            if ("text".equals(type) && map.get("text") instanceof String s) {
                if (!isNonTranslatableToken(s)) {
                    originals.add(s);
                    setters.add(val -> map.put("text", val));
                }
            } else if ("skillBarBlock".equals(type) && map.get("attrs") instanceof Map<?, ?> rawAttrs) {
                Map<String, Object> attrs = (Map<String, Object>) rawAttrs;
                if (attrs.get("label") instanceof String s && !isNonTranslatableToken(s)) {
                    originals.add(s);
                    setters.add(val -> attrs.put("label", val));
                }
            } else if ("badgeListBlock".equals(type) && map.get("attrs") instanceof Map<?, ?> rawAttrs) {
                Map<String, Object> attrs = (Map<String, Object>) rawAttrs;
                if (attrs.get("items") instanceof List<?> rawItems) {
                    List<Object> items = (List<Object>) rawItems;
                    for (int i = 0; i < items.size(); i++) {
                        final int idx = i;
                        if (items.get(i) instanceof String s && !isNonTranslatableToken(s)) {
                            originals.add(s);
                            setters.add(val -> items.set(idx, val));
                        }
                    }
                }
            } else if ("linkButtonBlock".equals(type) && map.get("attrs") instanceof Map<?, ?> rawAttrs) {
                Map<String, Object> attrs = (Map<String, Object>) rawAttrs;
                if (attrs.get("items") instanceof List<?> rawItems) {
                    for (Object itemObj : rawItems) {
                        if (itemObj instanceof Map<?, ?> rawItem) {
                            Map<String, Object> item = (Map<String, Object>) rawItem;
                            if (item.get("label") instanceof String s && !isNonTranslatableToken(s)) {
                                originals.add(s);
                                setters.add(val -> item.put("label", val));
                            }
                        }
                    }
                }
            }

            if (map.get("content") instanceof List<?> children) {
                for (Object child : children) {
                    collectTranslatableSlots(child, originals, setters);
                }
            }
        }
    }

    private boolean isNonTranslatableToken(String s) {
        if (s == null) return true;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return true;
        return NON_TRANSLATABLE_TOKEN_PATTERN.matcher(trimmed).matches();
    }

    private Object buildParseCvUserContent(
            ParseCvRequest request,
            List<String> images,
            String rawText,
            boolean includeImages
    ) throws IOException {
        StringBuilder userText = new StringBuilder();
        userText.append("Source type: ").append(request.sourceType() != null ? request.sourceType() : "pdf").append("\n");
        if (request.fileName() != null && !request.fileName().isBlank()) {
            userText.append("Filename: ").append(request.fileName()).append("\n");
        }
        if (request.colorHints() != null && !request.colorHints().isEmpty()) {
            userText.append("Sampled color hints: ").append(objectMapper.writeValueAsString(request.colorHints())).append("\n");
        }
        if (!rawText.isBlank()) {
            userText.append("\nExtracted text/HTML reference:\n").append(rawText);
        }

        if (includeImages && !images.isEmpty()) {
            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("type", "text", "text", userText.toString()));
            for (String dataUri : images) {
                parts.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", dataUri)
                ));
            }
            return parts;
        }
        return userText.toString();
    }

    private String sendChatCompletion(
            String effectiveBase,
            String apiKey,
            String model,
            String systemPrompt,
            Object userContent,
            double temperature,
            int maxTokens
    ) throws IOException, InterruptedException, RouterCallException {
        return sendChatCompletionMessages(
                effectiveBase,
                apiKey,
                model,
                List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                ),
                temperature,
                maxTokens
        );
    }

    private String sendChatCompletionMessages(
            String effectiveBase,
            String apiKey,
            String model,
            List<Map<String, Object>> messages,
            double temperature,
            int maxTokens
    ) throws IOException, InterruptedException, RouterCallException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("messages", messages);

        String jsonBody = objectMapper.writeValueAsString(body);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(effectiveBase + "/chat/completions"))
                .timeout(Duration.ofSeconds(95))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response;
        CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
            response = future.get(95, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new IOException("Router timeout after 95s (" + effectiveBase + "/chat/completions)", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof IOException ioEx) {
                throw ioEx;
            }
            throw new IOException(describeException(cause), cause);
        }

        String contentType = response.headers().firstValue("content-type").orElse("");
        String respBody = response.body() != null ? response.body() : "";
        log.info(
                "AI Router POST {}/chat/completions model={} status={} contentType={} bodyLen={}",
                effectiveBase,
                model,
                response.statusCode(),
                contentType,
                respBody.length()
        );

        if (response.statusCode() >= 400) {
            throw new RouterCallException(response.statusCode(), respBody);
        }

        return extractTextFromCompletionResponse(contentType, respBody);
    }

    private String extractTextFromCompletionResponse(String contentType, String respBody) throws IOException {
        String trimmed = respBody == null ? "" : respBody.trim();
        if (contentType.toLowerCase(Locale.ROOT).contains("text/event-stream") || trimmed.startsWith("data:")) {
            return extractTextFromSse(trimmed);
        }
        JsonNode root = objectMapper.readTree(trimmed);
        JsonNode choice = root.path("choices").path(0);
        String msgText = extractChoiceText(choice.path("message").path("content"));
        if (!msgText.isEmpty()) {
            return msgText;
        }
        return extractChoiceText(choice.path("delta").path("content"));
    }

    private String extractTextFromSse(String sseBody) {
        StringBuilder sb = new StringBuilder();
        String[] lines = sseBody.split("\\r?\\n");
        for (String line : lines) {
            String t = line.trim();
            if (!t.startsWith("data:")) {
                continue;
            }
            String payload = t.substring(5).trim();
            if (payload.isEmpty() || "[DONE]".equals(payload)) {
                continue;
            }
            try {
                JsonNode chunk = objectMapper.readTree(payload);
                JsonNode choice = chunk.path("choices").path(0);
                String deltaText = extractChoiceText(choice.path("delta").path("content"));
                if (!deltaText.isEmpty()) {
                    sb.append(deltaText);
                } else {
                    sb.append(extractChoiceText(choice.path("message").path("content")));
                }
            } catch (IOException ignored) {
                // Skip non-JSON SSE keepalive/comment payloads
            }
        }
        return sb.toString();
    }

    private String extractChoiceText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : contentNode) {
                if ("text".equals(part.path("type").asText())) {
                    sb.append(part.path("text").asText(""));
                }
            }
            return sb.toString();
        }
        return contentNode.asText("");
    }

    private String describeException(Throwable ex) {
        if (ex == null) return "Unknown error";
        String msg = ex.getMessage();
        if (msg != null && !msg.isBlank()) {
            return msg;
        }
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return ex.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    private ParseCvResponse buildParsedResponse(String rawOutput, List<String> notes) throws IOException {
        String cleaned = extractJsonObject(rawOutput);
        Map<String, Object> parsed = objectMapper.readValue(cleaned, MAP_TYPE);

        Object docObj = parsed.get("doc");
        if (!(docObj instanceof Map<?, ?> docMap) || !"doc".equals(docMap.get("type")) || !(docMap.get("content") instanceof List<?>)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI trả về cấu trúc JSON chưa hợp lệ (thiếu doc.content).");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> doc = (Map<String, Object>) docObj;
        @SuppressWarnings("unchecked")
        Map<String, Object> theme = parsed.get("theme") instanceof Map<?, ?>
                ? (Map<String, Object>) parsed.get("theme")
                : Map.of();

        return new ParseCvResponse(theme, doc, notes);
    }

    private String extractJsonObject(String raw) {
        if (raw == null) return "{}";
        String trimmed = stripCodeFences(raw);
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String extractJsonArray(String raw) {
        if (raw == null) return "[]";
        String trimmed = stripCodeFences(raw);
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String stripCodeFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    private String resolveEffectiveBaseUrl(String rawUrl) {
        String clean = stripTrailingSlash(rawUrl == null || rawUrl.isBlank() ? defaultBaseUrl : rawUrl.trim());
        boolean inDocker = Files.exists(Path.of("/.dockerenv"));
        if (inDocker && rewriteLocalhostInDocker) {
            clean = clean
                    .replaceFirst("^(https?://)localhost(?=[/:]|$)", "$1host.docker.internal")
                    .replaceFirst("^(https?://)127\\.0\\.0\\.1(?=[/:]|$)", "$1host.docker.internal");
        }
        return clean;
    }

    private String stripTrailingSlash(String url) {
        return url.replaceAll("/+$", "");
    }

    private boolean readBoolean(String key, boolean fallback) {
        return settingRepository.findById(key)
                .map(SystemSettingEntity::getSettingValue)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(Boolean::parseBoolean)
                .orElse(fallback);
    }

    private String readDbApiKey() {
        return settingRepository.findById(KEY_API_KEY)
                .map(SystemSettingEntity::getSettingValue)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(secretCryptoService::decrypt)
                .map(String::trim)
                .orElse("");
    }

    private String readApiKey() {
        String dbKey = readDbApiKey();
        if (!dbKey.isBlank()) {
            return dbKey;
        }
        return defaultApiKey != null ? defaultApiKey.trim() : "";
    }

    private String readString(String key, String fallback) {
        return settingRepository.findById(key)
                .map(SystemSettingEntity::getSettingValue)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .orElse(fallback);
    }

    private void saveSetting(String key, String value) {
        SystemSettingEntity entity = settingRepository.findById(key).orElseGet(SystemSettingEntity::new);
        entity.setSettingKey(key);
        entity.setSettingValue(value != null ? value : "");
        entity.setUpdatedAt(Instant.now());
        settingRepository.save(entity);
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) return "";
        String t = key.trim();
        if (t.length() <= 8) return "••••";
        return t.substring(0, 4) + "••••" + t.substring(t.length() - 4);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private static class RouterCallException extends Exception {
        final int statusCode;

        RouterCallException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
