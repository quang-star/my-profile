package dev.quang.portfolio.service;

import dev.quang.portfolio.entity.PageEntity;
import dev.quang.portfolio.repository.PageRepository;
import dev.quang.portfolio.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class ViewCounterService {

    private static final int MAX_CACHE_ENTRIES = 10_000;
    private static final Duration DEDUP_WINDOW = Duration.ofMinutes(30);

    private final PageRepository pageRepository;
    private final Map<String, Instant> recentViews = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Instant> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    public ViewCounterService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    /**
     * Records a deduplicated public page view in a short independent write transaction.
     * Skips owner/admin views and draft pages. Returns true if the view count was incremented.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordPublicView(PageEntity page, AuthenticatedUser currentUser) {
        if (page == null || "DRAFT".equalsIgnoreCase(page.getVisibility())) {
            return false;
        }
        if (currentUser != null && (currentUser.isSuperAdmin() || page.getOwnerId().equals(currentUser.id()))) {
            return false;
        }

        String viewerHash = resolveViewerHash();
        String dedupKey = page.getId() + "|" + viewerHash;
        Instant now = Instant.now();

        synchronized (recentViews) {
            Instant lastSeen = recentViews.get(dedupKey);
            if (lastSeen != null && lastSeen.plus(DEDUP_WINDOW).isAfter(now)) {
                return false;
            }
            recentViews.put(dedupKey, now);
        }

        pageRepository.incrementViewCount(page.getId());
        return true;
    }

    private String resolveViewerHash() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String ip = extractClientIp(req);
            String ua = req.getHeader("User-Agent");
            return sha256Hex(ip + "|" + (ua != null ? ua : ""));
        }
        return "anonymous";
    }

    private static String extractClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
    }

    private static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
