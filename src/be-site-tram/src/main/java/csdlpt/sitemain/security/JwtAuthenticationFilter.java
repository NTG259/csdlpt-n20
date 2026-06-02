package csdlpt.sitemain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            String username = jwtService.extractUsername(token);
            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtService.isTokenValid(token)) {
                CustomUserDetails userDetails = resolveUserDetails(username, token);
                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private CustomUserDetails resolveUserDetails(String username, String token) {
        CustomUserDetails databaseUser = customUserDetailsService.findByUsername(username)
                .orElse(null);

        String userId = jwtService.extractUserId(token);
        String role = jwtService.extractVaiTro(token);
        if (isBlank(userId) || isBlank(role)) {
            if (databaseUser == null) {
                throw new IllegalArgumentException("JWT missing required user claims");
            }
            return databaseUser;
        }

        String maKhoPhuTrach = firstNonBlank(
                jwtService.extractMaKhoPhuTrach(token),
                databaseUser == null ? null : databaseUser.getNguoiDung().getMaKhoPhuTrach()
        );
        String hoTen = databaseUser == null ? null : databaseUser.getNguoiDung().getHoTen();
        boolean enabled = databaseUser == null || databaseUser.isEnabled();

        return CustomUserDetails.fromTokenClaims(
                username,
                UUID.fromString(userId),
                jwtService.extractMaKhuVuc(token),
                role,
                maKhoPhuTrach,
                hoTen,
                enabled
        );
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
