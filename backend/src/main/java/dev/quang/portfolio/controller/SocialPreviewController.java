package dev.quang.portfolio.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.quang.portfolio.entity.PageEntity;
import dev.quang.portfolio.entity.UserEntity;
import dev.quang.portfolio.repository.PageRepository;
import dev.quang.portfolio.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

@RestController
@RequestMapping("/api/public/og")
public class SocialPreviewController {

    private static final MediaType HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    private final UserRepository userRepository;
    private final PageRepository pageRepository;
    private final ObjectMapper objectMapper;

    public SocialPreviewController(
            UserRepository userRepository,
            PageRepository pageRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.pageRepository = pageRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping({"/home", "/home/"})
    @Transactional(readOnly = true)
    public ResponseEntity<String> getHomeOgHtml(HttpServletRequest request) {
        PageEntity featured = pageRepository.findFirstByHomeFeaturedTrueAndVisibilityIgnoreCase("PUBLIC")
                .orElse(null);
        UserEntity owner = featured != null
                ? userRepository.findById(featured.getOwnerId()).orElse(null)
                : null;

        String rawTitle = "portfolio.studio — Thư viện mẫu Portfolio & CV";
        String rawDesc = "Xem trước các mẫu Portfolio & CV công khai, tùy biến trực quan, tạo đường dẫn cá nhân và xuất PDF A4.";
        String baseUrl = resolveBaseUrl(request);
        String ogImage = (featured != null && owner != null) ? resolveOgImage(featured, owner, baseUrl) : null;

        return buildSimpleOgHtml(rawTitle, rawDesc, baseUrl, "/", ogImage);
    }

    @GetMapping({"/u/{username}", "/u/{username}/"})
    @Transactional(readOnly = true)
    public ResponseEntity<String> getPrimaryOgHtml(
            @PathVariable String username,
            HttpServletRequest request
    ) {
        UserEntity owner = findActiveUser(username);
        PageEntity page = pageRepository.findFirstByOwnerIdAndPrimaryTrue(owner.getId())
                .filter(p -> "PUBLIC".equalsIgnoreCase(p.getVisibility()))
                .orElseThrow(() -> new NoSuchElementException("Public page not found for user: " + username));

        String targetPath = "/@" + UriUtils.encodePathSegment(owner.getUsername(), StandardCharsets.UTF_8);
        return buildOgResponse(owner, page, targetPath, request);
    }

    @GetMapping({"/u/{username}/{slug}", "/u/{username}/{slug}/"})
    @Transactional(readOnly = true)
    public ResponseEntity<String> getSlugOgHtml(
            @PathVariable String username,
            @PathVariable String slug,
            HttpServletRequest request
    ) {
        UserEntity owner = findActiveUser(username);
        PageEntity page = pageRepository.findByOwnerIdAndSlug(owner.getId(), slug.trim().toLowerCase(Locale.ROOT))
                .filter(p -> !"DRAFT".equalsIgnoreCase(p.getVisibility()))
                .orElseThrow(() -> new NoSuchElementException("Page not found"));

        String targetPath = "/@" + UriUtils.encodePathSegment(owner.getUsername(), StandardCharsets.UTF_8)
                + "/" + UriUtils.encodePathSegment(page.getSlug(), StandardCharsets.UTF_8);
        return buildOgResponse(owner, page, targetPath, request);
    }

    private UserEntity findActiveUser(String username) {
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
    }

    private ResponseEntity<String> buildOgResponse(
            UserEntity owner,
            PageEntity page,
            String targetPath,
            HttpServletRequest request
    ) {
        String rawTitle = page.getTitle() != null && !page.getTitle().isBlank()
                ? page.getTitle().trim()
                : owner.getDisplayName() + " — Portfolio";
        String rawDesc = page.getDescription() != null && !page.getDescription().isBlank()
                ? page.getDescription().trim()
                : owner.getDisplayName() + " (@" + owner.getUsername() + ")";

        String baseUrl = resolveBaseUrl(request);
        String ogImage = resolveOgImage(page, owner, baseUrl);
        return buildSimpleOgHtml(rawTitle, rawDesc, baseUrl, targetPath, ogImage);
    }

    private ResponseEntity<String> buildSimpleOgHtml(
            String rawTitle,
            String rawDesc,
            String baseUrl,
            String targetPath,
            String ogImage
    ) {
        String targetUrl = baseUrl + targetPath;

        String escTitle = HtmlUtils.htmlEscape(rawTitle);
        String escDesc = HtmlUtils.htmlEscape(rawDesc);
        String escTargetUrl = HtmlUtils.htmlEscape(targetUrl);
        String escTargetPath = HtmlUtils.htmlEscape(targetPath);

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\" />");
        html.append("<title>").append(escTitle).append("</title>");
        html.append("<meta name=\"description\" content=\"").append(escDesc).append("\" />");
        html.append("<meta property=\"og:type\" content=\"profile\" />");
        html.append("<meta property=\"og:title\" content=\"").append(escTitle).append("\" />");
        html.append("<meta property=\"og:description\" content=\"").append(escDesc).append("\" />");
        html.append("<meta property=\"og:url\" content=\"").append(escTargetUrl).append("\" />");
        if (ogImage != null && !ogImage.isBlank()) {
            String escImage = HtmlUtils.htmlEscape(ogImage);
            html.append("<meta property=\"og:image\" content=\"").append(escImage).append("\" />");
            html.append("<meta name=\"twitter:card\" content=\"summary_large_image\" />");
            html.append("<meta name=\"twitter:image\" content=\"").append(escImage).append("\" />");
        } else {
            html.append("<meta name=\"twitter:card\" content=\"summary\" />");
        }
        html.append("<meta name=\"twitter:title\" content=\"").append(escTitle).append("\" />");
        html.append("<meta name=\"twitter:description\" content=\"").append(escDesc).append("\" />");
        html.append("<link rel=\"canonical\" href=\"").append(escTargetUrl).append("\" />");
        html.append("<script>window.location.replace(\"").append(escapeJs(targetPath)).append("\");</script>");
        html.append("</head><body><p>Redirecting to <a href=\"").append(escTargetPath).append("\">")
                .append(escTitle).append("</a>...</p></body></html>");

        return ResponseEntity.ok().contentType(HTML_UTF8).body(html.toString());
    }

    private String resolveOgImage(PageEntity page, UserEntity owner, String baseUrl) {
        String raw = null;
        if (page.getThemeJson() != null && !page.getThemeJson().isBlank()) {
            try {
                JsonNode theme = objectMapper.readTree(page.getThemeJson());
                if (theme != null && theme.hasNonNull("ogImageUrl")) {
                    String candidate = theme.get("ogImageUrl").asText("").trim();
                    if (!candidate.isEmpty()) {
                        raw = candidate;
                    }
                }
            } catch (Exception ignored) {
                // Ignore malformed themeJson
            }
        }
        if (raw == null && owner.getAvatarUrl() != null && !owner.getAvatarUrl().isBlank()) {
            raw = owner.getAvatarUrl().trim();
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (raw.startsWith("/")) {
            return baseUrl + raw;
        }
        return raw;
    }

    private static String resolveBaseUrl(HttpServletRequest request) {
        try {
            return ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static String escapeJs(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c");
    }
}
