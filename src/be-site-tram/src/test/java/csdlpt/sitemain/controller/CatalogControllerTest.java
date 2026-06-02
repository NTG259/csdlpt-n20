package csdlpt.sitemain.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.config.SecurityConfig;
import csdlpt.sitemain.dto.response.BrandResponse;
import csdlpt.sitemain.dto.response.CategoryResponse;
import csdlpt.sitemain.dto.response.ProductDetailResponse;
import csdlpt.sitemain.dto.response.ProductListItemResponse;
import csdlpt.sitemain.dto.response.RegionResponse;
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
    private CategoryService categoryService;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private RegionService regionService;

    @MockitoBean
    private TonKhoService tonKhoService;

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
