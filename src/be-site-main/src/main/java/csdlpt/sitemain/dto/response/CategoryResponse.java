package csdlpt.sitemain.dto.response;

public record CategoryResponse(
        String maDanhMuc,
        String tenDanhMuc,
        String maDanhMucCha,
        String moTa,
        Boolean trangThai
) {
}
