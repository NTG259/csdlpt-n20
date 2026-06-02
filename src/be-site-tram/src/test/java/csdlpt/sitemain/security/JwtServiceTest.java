package csdlpt.sitemain.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import csdlpt.sitemain.domain.entity.KhuVuc;
import csdlpt.sitemain.domain.entity.NguoiDung;
import csdlpt.sitemain.domain.enums.VaiTro;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    @Test
    void shouldGenerateTokenWithExpectedClaims() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "jwt-secret-key-for-tests-with-at-least-32-bytes");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 60_000L);

        KhuVuc khuVuc = new KhuVuc();
        khuVuc.setMaKhuVuc("KV01");

        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setMaND(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        nguoiDung.setEmail("user@example.com");
        nguoiDung.setVaiTro(VaiTro.USER);
        nguoiDung.setKhuVuc(khuVuc);

        String token = jwtService.generateToken(nguoiDung);

        assertEquals("user@example.com", jwtService.extractUsername(token));
        assertEquals(
                "11111111-1111-1111-1111-111111111111",
                jwtService.extractClaim(token, claims -> claims.get("userId", String.class))
        );
        assertEquals("KV01", jwtService.extractClaim(token, claims -> claims.get("maKhuVuc", String.class)));
        assertEquals("USER", jwtService.extractClaim(token, claims -> claims.get("vaiTro", String.class)));
        assertTrue(jwtService.isTokenValid(token));
    }
}
