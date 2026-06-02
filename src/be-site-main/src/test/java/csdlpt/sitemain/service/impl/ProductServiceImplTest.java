package csdlpt.sitemain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.domain.entity.DanhMuc;
import csdlpt.sitemain.domain.entity.SanPhamCore;
import csdlpt.sitemain.domain.entity.SanPhamDetail;
import csdlpt.sitemain.domain.entity.ThuongHieu;
import csdlpt.sitemain.dto.projection.ProductListItemView;
import csdlpt.sitemain.dto.response.ProductDetailResponse;
import csdlpt.sitemain.dto.response.ProductListItemResponse;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.DanhMucRepository;
import csdlpt.sitemain.repository.SanPhamCoreRepository;
import csdlpt.sitemain.repository.SanPhamDetailRepository;
import csdlpt.sitemain.repository.ThuongHieuRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private SanPhamCoreRepository sanPhamCoreRepository;

    @Mock
    private SanPhamDetailRepository sanPhamDetailRepository;

    @Mock
    private DanhMucRepository danhMucRepository;

    @Mock
    private ThuongHieuRepository thuongHieuRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getProductsShouldMapProjectionToPageResponse() {
        PageRequest pageable = PageRequest.of(0, 10);
        ProductListItemView projection = new ProductListItemView() {
            @Override
            public String getMaSP() {
                return "SP01";
            }

            @Override
            public String getTenSP() {
                return "Laptop";
            }

            @Override
            public BigDecimal getGiaBan() {
                return new BigDecimal("15000000");
            }

            @Override
            public String getDonViTinh() {
                return "cái";
            }

            @Override
            public String getHinhAnh() {
                return "image.png";
            }

            @Override
            public Boolean getTrangThai() {
                return true;
            }

            @Override
            public String getTenDanhMuc() {
                return "Điện tử";
            }

            @Override
            public String getTenThuongHieu() {
                return "OpenAI";
            }
        };

        when(sanPhamCoreRepository.searchProjection("DM01", "TH01", true, pageable))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

        PageResponse<ProductListItemResponse> response = productService.getProducts(" DM01 ", " TH01 ", true, pageable);

        assertEquals(1, response.items().size());
        assertEquals(1, response.totalElements());
        assertEquals("SP01", response.items().getFirst().maSP());
        assertEquals("cái", response.items().getFirst().donViTinh());
        assertEquals("OpenAI", response.items().getFirst().tenThuongHieu());
    }

    @Test
    void getProductDetailShouldMergeCoreAndDetail() {
        DanhMuc danhMuc = new DanhMuc();
        danhMuc.setMaDanhMuc("DM01");
        danhMuc.setTenDanhMuc("Điện tử");

        ThuongHieu thuongHieu = new ThuongHieu();
        thuongHieu.setMaThuongHieu("TH01");
        thuongHieu.setTenThuongHieu("OpenAI");

        SanPhamCore sanPhamCore = new SanPhamCore();
        sanPhamCore.setMaSP("SP01");
        sanPhamCore.setTenSP("Laptop");
        sanPhamCore.setGiaBan(new BigDecimal("15000000"));
        sanPhamCore.setDonViTinh("cái");
        sanPhamCore.setHinhAnh("image.png");
        sanPhamCore.setTrangThai(true);
        sanPhamCore.setNgayTao(LocalDateTime.of(2026, 1, 1, 10, 0));
        sanPhamCore.setDanhMuc(danhMuc);
        sanPhamCore.setThuongHieu(thuongHieu);

        SanPhamDetail sanPhamDetail = new SanPhamDetail();
        sanPhamDetail.setMaSP("SP01");
        sanPhamDetail.setMoTa("Mô tả");
        sanPhamDetail.setThongSoKyThuat("{\"cpu\":\"x1\"}");

        when(sanPhamCoreRepository.findDetailById("SP01")).thenReturn(Optional.of(sanPhamCore));
        when(sanPhamDetailRepository.findById("SP01")).thenReturn(Optional.of(sanPhamDetail));

        ProductDetailResponse response = productService.getProductDetail(" SP01 ");

        assertEquals("SP01", response.maSP());
        assertEquals("Điện tử", response.tenDanhMuc());
        assertEquals("OpenAI", response.tenThuongHieu());
        assertEquals("{\"cpu\":\"x1\"}", response.thongSoKyThuat());
    }

    @Test
    void getProductDetailShouldReturnNullDetailFieldsWhenDetailMissing() {
        DanhMuc danhMuc = new DanhMuc();
        danhMuc.setMaDanhMuc("DM01");
        danhMuc.setTenDanhMuc("Điện tử");

        ThuongHieu thuongHieu = new ThuongHieu();
        thuongHieu.setMaThuongHieu("TH01");
        thuongHieu.setTenThuongHieu("OpenAI");

        SanPhamCore sanPhamCore = new SanPhamCore();
        sanPhamCore.setMaSP("SP01");
        sanPhamCore.setTenSP("Laptop");
        sanPhamCore.setGiaBan(new BigDecimal("15000000"));
        sanPhamCore.setDonViTinh("cái");
        sanPhamCore.setTrangThai(true);
        sanPhamCore.setDanhMuc(danhMuc);
        sanPhamCore.setThuongHieu(thuongHieu);

        when(sanPhamCoreRepository.findDetailById("SP01")).thenReturn(Optional.of(sanPhamCore));
        when(sanPhamDetailRepository.findById("SP01")).thenReturn(Optional.empty());

        ProductDetailResponse response = productService.getProductDetail("SP01");

        assertNull(response.moTa());
        assertNull(response.thongSoKyThuat());
    }

    @Test
    void getProductDetailShouldThrowWhenProductNotFound() {
        when(sanPhamCoreRepository.findDetailById("SP99")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductDetail("SP99"));
    }
}
