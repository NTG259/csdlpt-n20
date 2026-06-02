package csdlpt.sitemain.dto.response;

public record RegisterResponse(
        String userId,
        String hoTen,
        String email,
        String maKhuVuc
) {
}
