package csdlpt.sitemain.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import csdlpt.sitemain.config.SecurityConfig;
import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.DoanhThuTheoKhoResponse;
import csdlpt.sitemain.dto.response.DoanhThuTheoVungResponse;
import csdlpt.sitemain.dto.response.DoanhThuToanHeThongResponse;
import csdlpt.sitemain.dto.response.ThongKeDoanhThuResponse;
import csdlpt.sitemain.security.CustomUserDetailsService;
import csdlpt.sitemain.security.JwtAuthenticationFilter;
import csdlpt.sitemain.security.JwtService;
import csdlpt.sitemain.service.ThongKeService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminThongKeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminThongKeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ThongKeService thongKeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldReturnRevenueStatsForAdminAndBindFilters() throws Exception {
        when(thongKeService.thongKeDoanhThu(any(ThongKeDoanhThuFilter.class))).thenReturn(new ThongKeDoanhThuResponse(
                List.of(new DoanhThuTheoKhoResponse(
                        "BAC",
                        "KV01",
                        "KB01",
                        "Kho Hà Nội",
                        2,
                        2,
                        5,
                        BigDecimal.valueOf(1500000)
                )),
                List.of(new DoanhThuTheoVungResponse(
                        "KV01",
                        2,
                        2,
                        1,
                        5,
                        BigDecimal.valueOf(1500000)
                )),
                new DoanhThuToanHeThongResponse(
                        2,
                        2,
                        1,
                        5,
                        BigDecimal.valueOf(1500000)
                )
        ));

        mockMvc.perform(get("/api/admin/thong-ke/doanh-thu")
                        .with(user("admin").roles("ADMIN"))
                        .param("tuNgay", "2026-06-01")
                        .param("denNgay", "2026-06-02")
                        .param("maKho", "KB01")
                        .param("maKhuVuc", "KV01")
                        .param("maSP", "SP01")
                        .param("chiTinhDaXuat", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.theoKho[0].maKhoXuat").value("KB01"))
                .andExpect(jsonPath("$.data.toanHeThong.tongDoanhThu").value(1500000));

        ArgumentCaptor<ThongKeDoanhThuFilter> captor = ArgumentCaptor.forClass(ThongKeDoanhThuFilter.class);
        verify(thongKeService).thongKeDoanhThu(captor.capture());
        ThongKeDoanhThuFilter filter = captor.getValue();

        assertThat(filter.tuNgay()).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0));
        assertThat(filter.denNgay()).isEqualTo(LocalDateTime.of(2026, 6, 2, 0, 0));
        assertThat(filter.maKho()).isEqualTo("KB01");
        assertThat(filter.chiTinhDaXuat()).isFalse();
    }

    @Test
    void shouldForbidRevenueStatsWhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/thong-ke/doanh-thu").with(user("user").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
