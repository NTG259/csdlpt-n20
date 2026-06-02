package csdlpt.sitemain.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import csdlpt.sitemain.config.SecurityConfig;
import csdlpt.sitemain.domain.entity.NguoiDung;
import csdlpt.sitemain.domain.enums.VaiTro;
import csdlpt.sitemain.dto.request.CapNhatSoLuongRequest;
import csdlpt.sitemain.dto.request.ThemVaoGioRequest;
import csdlpt.sitemain.dto.response.ChiTietGioHangResponse;
import csdlpt.sitemain.dto.response.GioHangResponse;
import csdlpt.sitemain.security.CustomUserDetails;
import csdlpt.sitemain.security.CustomUserDetailsService;
import csdlpt.sitemain.security.JwtAuthenticationFilter;
import csdlpt.sitemain.security.JwtService;
import csdlpt.sitemain.service.GioHangService;
import csdlpt.sitemain.service.TonKhoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GioHangController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class GioHangControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private GioHangService gioHangService;

    @MockitoBean
    private TonKhoService tonKhoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private CustomUserDetails userDetails() {
        NguoiDung nd = new NguoiDung();
        nd.setMaND(USER_ID);
        nd.setEmail("test@example.com");
        nd.setMatKhau("hashed");
        nd.setHoTen("Test User");
        nd.setTrangThai(true);
        nd.setVaiTro(VaiTro.USER);
        return new CustomUserDetails(nd);
    }

    private GioHangResponse gioHangResponse() {
        ChiTietGioHangResponse ct = new ChiTietGioHangResponse(
                "SP001", "Laptop", "/img/sp001.jpg", "Cái",
                2, new BigDecimal("15000000"), new BigDecimal("30000000"), 10
        );
        return new GioHangResponse(List.of(ct), List.of(), 2, new BigDecimal("30000000"));
    }

    // ── GET /api/cart ────────────────────────────────────────────────────────────

    @Test
    void getGioHang_khongCoToken_tra401() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void getGioHang_coToken_tra200VoiDuLieu() throws Exception {
        when(gioHangService.getGioHang(USER_ID)).thenReturn(gioHangResponse());

        mockMvc.perform(get("/api/cart").with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tongSoLuong").value(2))
                .andExpect(jsonPath("$.data.tongTien").value(30000000))
                .andExpect(jsonPath("$.data.sanPhamHopLe[0].maSP").value("SP001"))
                .andExpect(jsonPath("$.data.sanPhamHopLe[0].giaBan").value(15000000))
                .andExpect(jsonPath("$.data.sanPhamHopLe[0].soLuongKhaDung").value(10))
                .andExpect(jsonPath("$.data.sanPhamHetHang").isEmpty());
    }

    // ── POST /api/cart/items ─────────────────────────────────────────────────────

    @Test
    void themVaoGio_khongCoToken_tra401() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ThemVaoGioRequest("SP001", 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void themVaoGio_bodyHopLe_tra200() throws Exception {
        when(gioHangService.themVaoGio(eq(USER_ID), any())).thenReturn(gioHangResponse());

        mockMvc.perform(post("/api/cart/items")
                        .with(user(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ThemVaoGioRequest("SP001", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã thêm sản phẩm vào giỏ hàng"))
                .andExpect(jsonPath("$.data.tongSoLuong").value(2));
    }

    @Test
    void themVaoGio_maSPRong_tra400() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .with(user(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ThemVaoGioRequest("", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void themVaoGio_soLuongBangZero_tra400() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .with(user(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ThemVaoGioRequest("SP001", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    // ── PUT /api/cart/items/{maSP} ───────────────────────────────────────────────

    @Test
    void capNhatSoLuong_coToken_tra200() throws Exception {
        when(gioHangService.capNhatSoLuong(eq(USER_ID), eq("SP001"), any())).thenReturn(gioHangResponse());

        mockMvc.perform(put("/api/cart/items/SP001")
                        .with(user(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CapNhatSoLuongRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã cập nhật số lượng"));
    }

    @Test
    void capNhatSoLuong_soLuongKhongHopLe_tra400() throws Exception {
        mockMvc.perform(put("/api/cart/items/SP001")
                        .with(user(userDetails()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CapNhatSoLuongRequest(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void capNhatSoLuong_khongCoToken_tra401() throws Exception {
        mockMvc.perform(put("/api/cart/items/SP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CapNhatSoLuongRequest(5))))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/cart/items/{maSP} ────────────────────────────────────────────

    @Test
    void xoaSanPham_coToken_tra200() throws Exception {
        mockMvc.perform(delete("/api/cart/items/SP001").with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa sản phẩm khỏi giỏ hàng"));

        verify(gioHangService).xoaSanPham(USER_ID, "SP001");
    }

    @Test
    void xoaSanPham_khongCoToken_tra401() throws Exception {
        mockMvc.perform(delete("/api/cart/items/SP001"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/cart ─────────────────────────────────────────────────────────

    @Test
    void xoaGioHang_coToken_tra200() throws Exception {
        mockMvc.perform(delete("/api/cart").with(user(userDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã xóa toàn bộ giỏ hàng"));

        verify(gioHangService).xoaGioHang(USER_ID);
    }

    @Test
    void xoaGioHang_khongCoToken_tra401() throws Exception {
        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isUnauthorized());
    }
}
