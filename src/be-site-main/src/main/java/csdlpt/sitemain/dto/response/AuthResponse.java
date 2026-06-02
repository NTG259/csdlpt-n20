package csdlpt.sitemain.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        Long expiresIn,
        String userId,
        String hoTen,
        String email,
        String maKhuVuc,
        String vaiTro
) {
}
