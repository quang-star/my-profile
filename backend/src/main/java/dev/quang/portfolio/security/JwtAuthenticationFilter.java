package dev.quang.portfolio.security;

import dev.quang.portfolio.entity.UserEntity;
import dev.quang.portfolio.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    Claims claims = jwtService.parseClaims(token);
                    String userId = claims.getSubject();
                    if (userId != null && !userId.isBlank()) {
                        UserEntity user = userRepository.findById(userId).orElse(null);
                        if (user != null && "ACTIVE".equalsIgnoreCase(user.getStatus())) {
                            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                            if ("SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                            } else {
                                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                            }
                            AuthenticatedUser principal = new AuthenticatedUser(
                                    user.getId(),
                                    user.getUsername(),
                                    user.getRole()
                            );
                            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                } catch (Exception ignored) {
                    // Invalid or expired JWT -> leave unauthenticated
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
