package csdlpt.sitemain.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.config.SecurityConfig;
import csdlpt.sitemain.dto.request.ProductUpsertRequest;
import csdlpt.sitemain.dto.response.BrandResponse;
import csdlpt.sitemain.dto.response.CategoryResponse;
import csdlpt.sitemain.dto.response.ProductDetailResponse;
import csdlpt.sitemain.dto.response.ProductListItemResponse;
import csdlpt.sitemain.dto.response.RegionResponse;
import csdlpt.sitemain.dto.response.TonKhoChiTietKhoResponse;
import csdlpt.sitemain.dto.response.TonKhoHeThongResponse;
import csdlpt.sitemain.security.CustomUserDetailsService;
import csdlpt.sitemain.security.JwtAuthenticationFilter;
import csdlpt.sitemain.security.JwtService;
import csdlpt.sitemain.service.BrandService;
import csdlpt.sitemain.service.CategoryService;
import csdlpt.sitemain.service.ProductService;
import csdlpt.sitemain.service.RegionService;
import csdlpt.sitemain.service.TonKhoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
        ProductController.class,
        CategoryController.class,
        BrandController.class,
        RegionController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private TonKhoService tonKhoService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private RegionService regionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldBindProductsFilterAndPageable() throws Exception {
        PageResponse<ProductListItemResponse> response = new PageResponse<>(
                List.of(new ProductListItemResponse(
                        "SP01",
                        "Laptop A",
                        BigDecimal.valueOf(19990000),
                        "Cái",
                        "https://example.com/sp01.jpg",
                        true,
                        "Laptop",
                        "Dell"
                )),
                1,
                5,
                1,
                1,
                true
        );

        when(productService.getProducts(eq("DM01"), eq("TH01"), eq(true), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "tenSP,asc")
                        .param("maDanhMuc", "DM01")
                        .param("maThuongHieu", "TH01")
                        .param("trangThai", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].maSP").value("SP01"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getProducts(eq("DM01"), eq("TH01"), eq(true), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("tenSP")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("tenSP").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldUseDefaultProductsPageableWhenParamsAreMissing() throws Exception {
        when(productService.getProducts(isNull(), isNull(), isNull(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getProducts(isNull(), isNull(), isNull(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("ngayTao")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("ngayTao").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldReturnProductDetailWithoutToken() throws Exception {
        when(productService.getProductDetail("SP01")).thenReturn(new ProductDetailResponse(
                "SP01",
                "Laptop A",
                BigDecimal.valueOf(19990000),
                "Cái",
                "https://example.com/sp01.jpg",
                true,
                LocalDateTime.of(2026, 6, 2, 10, 0),
                "DM01",
                "Laptop",
                "TH01",
                "Dell",
                "Mô tả",
                "{\"cpu\":\"i7\"}"
        ));

        mockMvc.perform(get("/api/products/SP01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maSP").value("SP01"))
                .andExpect(jsonPath("$.data.tenThuongHieu").value("Dell"));
    }

    @Test
    void shouldAllowAdminToCreateProduct() throws Exception {
        when(productService.createProduct(any(ProductUpsertRequest.class))).thenReturn(new ProductDetailResponse(
                "SP02",
                "Laptop B",
                BigDecimal.valueOf(29990000),
                "Cái",
                "https://example.com/sp02.jpg",
                true,
                LocalDateTime.of(2026, 6, 2, 11, 0),
                "DM01",
                "Laptop",
                "TH01",
                "Dell",
                "Mô tả",
                "{\"cpu\":\"i9\"}"
        ));

        mockMvc.perform(post("/api/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maSP": "SP02",
                                  "tenSP": "Laptop B",
                                  "maDanhMuc": "DM01",
                                  "maThuongHieu": "TH01",
                                  "giaBan": 29990000,
                                  "donViTinh": "Cái",
                                  "hinhAnh": "https://example.com/sp02.jpg",
                                  "trangThai": true,
                                  "moTa": "Mô tả",
                                  "thongSoKyThuat": "{\\"cpu\\":\\"i9\\"}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maSP").value("SP02"));

        verify(productService).createProduct(any(ProductUpsertRequest.class));
    }

    @Test
    void shouldForbidCatalogWriteWhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/brands/TH01").with(user("user").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void shouldReturnSystemInventoryWithoutToken() throws Exception {
        when(tonKhoService.getTonKhoToanHeThong("SP01")).thenReturn(new TonKhoHeThongResponse(
                "SP01",
                "Laptop A",
                50,
                8,
                42,
                2,
                List.of(
                        new TonKhoChiTietKhoResponse("SITE_BAC", "KB01", "Kho Ha Noi", 30, 5, 25),
                        new TonKhoChiTietKhoResponse("SITE_NAM", "KN01", "Kho HCM", 20, 3, 17)
                )
        ));

        mockMvc.perform(get("/api/products/SP01/ton-kho"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maSP").value("SP01"))
                .andExpect(jsonPath("$.data.tongKhaDung").value(42))
                .andExpect(jsonPath("$.data.soLuongKho").value(2))
                .andExpect(jsonPath("$.data.chiTietKho[0].site").value("SITE_BAC"))
                .andExpect(jsonPath("$.data.chiTietKho[1].soLuongKhaDung").value(17));
    }

    @Test
    void shouldReturnCategoriesWithoutToken() throws Exception {
        when(categoryService.getAll()).thenReturn(List.of(
                new CategoryResponse("DM01", "Laptop", null, "Mô tả", true)
        ));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].maDanhMuc").value("DM01"));
    }

    @Test
    void shouldReturnBrandsWithoutToken() throws Exception {
        when(brandService.getAll()).thenReturn(List.of(
                new BrandResponse("TH01", "Dell", true)
        ));

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].tenThuongHieu").value("Dell"));
    }

    @Test
    void shouldReturnRegionsWithoutToken() throws Exception {
        when(regionService.getAll()).thenReturn(List.of(
                new RegionResponse("KV01", "Miền Bắc")
        ));

        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].maKhuVuc").value("KV01"));
    }
}
