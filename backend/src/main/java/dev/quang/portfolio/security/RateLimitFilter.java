package dev.quang.portfolio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_BUCKETS = 20_000;
    private static final Pattern PAGE_CONTACT_PATTERN = Pattern.compile("^/api/public/pages/[^/]+/contact$");

    private record RateLimitRule(String name, int maxRequests, Duration window) {}

    private static final RateLimitRule REGISTER_RULE = new RateLimitRule("register", 8, Duration.ofHours(1));
    private static final RateLimitRule LOGIN_RULE = new RateLimitRule("login", 12, Duration.ofMinutes(5));
    private static final RateLimitRule GOOGLE_RULE = new RateLimitRule("google", 15, Duration.ofMinutes(5));
    private static final RateLimitRule FORGOT_PASSWORD_RULE = new RateLimitRule("forgot-password", 5, Duration.ofMinutes(15));
    private static final RateLimitRule RESET_PASSWORD_RULE = new RateLimitRule("reset-password", 10, Duration.ofMinutes(15));
    private static final RateLimitRule CONTACT_RULE = new RateLimitRule("contact", 5, Duration.ofMinutes(10));

    private final boolean enabled;
    private final Map<String, Deque<Instant>> buckets = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Deque<Instant>> eldest) {
                    return size() > MAX_BUCKETS;
                }
            }
    );

    public RateLimitFilter(@Value("${app.rate-limit.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        RateLimitRule rule = matchRule(path);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String key = rule.name() + "|" + clientIp;
        if (!tryAcquire(key, rule)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static RateLimitRule matchRule(String path) {
        if (path == null) return null;
        return switch (path) {
            case "/api/auth/register" -> REGISTER_RULE;
            case "/api/auth/login" -> LOGIN_RULE;
            case "/api/auth/google" -> GOOGLE_RULE;
            case "/api/auth/forgot-password" -> FORGOT_PASSWORD_RULE;
            case "/api/auth/reset-password" -> RESET_PASSWORD_RULE;
            default -> PAGE_CONTACT_PATTERN.matcher(path).matches() ? CONTACT_RULE : null;
        };
    }

    private boolean tryAcquire(String key, RateLimitRule rule) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(rule.window());
        synchronized (buckets) {
            Deque<Instant> timestamps = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
            while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= rule.maxRequests()) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
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
}
