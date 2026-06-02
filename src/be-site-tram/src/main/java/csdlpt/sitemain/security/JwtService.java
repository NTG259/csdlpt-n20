package csdlpt.sitemain.security;

import csdlpt.sitemain.domain.entity.NguoiDung;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(NguoiDung user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getMaND().toString());
        claims.put("maKhuVuc", user.getKhuVuc() == null ? null : user.getKhuVuc().getMaKhuVuc());
        claims.put("vaiTro", user.getVaiTro().name());
        claims.put("maKhoPhuTrach", user.getMaKhoPhuTrach());

        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && isTokenValid(token);
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractMaKhuVuc(String token) {
        return extractClaim(token, claims -> claims.get("maKhuVuc", String.class));
    }

    public String extractVaiTro(String token) {
        return extractClaim(token, claims -> claims.get("vaiTro", String.class));
    }

    public String extractMaKhoPhuTrach(String token) {
        return extractClaim(token, claims -> claims.get("maKhoPhuTrach", String.class));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = resolveKeyBytes(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveKeyBytes(String secretValue) {
        try {
            byte[] decoded = Decoders.BASE64.decode(secretValue);
            return decoded.length >= 32 ? decoded : secretValue.getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            return secretValue.getBytes(StandardCharsets.UTF_8);
        }
    }
}
