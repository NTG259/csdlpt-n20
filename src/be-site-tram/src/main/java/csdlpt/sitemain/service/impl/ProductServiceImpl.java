package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.domain.entity.SanPhamCore;
import csdlpt.sitemain.domain.entity.SanPhamDetail;
import csdlpt.sitemain.dto.projection.ProductListItemView;
import csdlpt.sitemain.dto.response.ProductDetailResponse;
import csdlpt.sitemain.dto.response.ProductListItemResponse;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.SanPhamCoreRepository;
import csdlpt.sitemain.repository.SanPhamDetailRepository;
import csdlpt.sitemain.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final SanPhamCoreRepository sanPhamCoreRepository;
    private final SanPhamDetailRepository sanPhamDetailRepository;

    public ProductServiceImpl(
            SanPhamCoreRepository sanPhamCoreRepository,
            SanPhamDetailRepository sanPhamDetailRepository
    ) {
        this.sanPhamCoreRepository = sanPhamCoreRepository;
        this.sanPhamDetailRepository = sanPhamDetailRepository;
    }

    @Override
    public PageResponse<ProductListItemResponse> getProducts(
            String maDanhMuc,
            String maThuongHieu,
            Boolean trangThai,
            Pageable pageable
    ) {
        Page<ProductListItemResponse> page = sanPhamCoreRepository.searchProjection(
                normalizeNullable(maDanhMuc),
                normalizeNullable(maThuongHieu),
                trangThai,
                pageable
        ).map(this::toProductListItemResponse);

        return PageResponse.from(page);
    }

    @Override
    public ProductDetailResponse getProductDetail(String maSP) {
        String normalizedMaSP = normalizeRequired(maSP);
        SanPhamCore sanPhamCore = sanPhamCoreRepository.findDetailById(normalizedMaSP)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với mã: " + normalizedMaSP));

        SanPhamDetail sanPhamDetail = sanPhamDetailRepository.findById(normalizedMaSP).orElse(null);
        return toProductDetailResponse(sanPhamCore, sanPhamDetail);
    }

    private ProductListItemResponse toProductListItemResponse(ProductListItemView view) {
        return new ProductListItemResponse(
                view.getMaSP(),
                view.getTenSP(),
                view.getGiaBan(),
                view.getDonViTinh(),
                view.getHinhAnh(),
                view.getTrangThai(),
                view.getTenDanhMuc(),
                view.getTenThuongHieu()
        );
    }

    private ProductDetailResponse toProductDetailResponse(SanPhamCore sanPhamCore, SanPhamDetail sanPhamDetail) {
        return new ProductDetailResponse(
                sanPhamCore.getMaSP(),
                sanPhamCore.getTenSP(),
                sanPhamCore.getGiaBan(),
                sanPhamCore.getDonViTinh(),
                sanPhamCore.getHinhAnh(),
                sanPhamCore.getTrangThai(),
                sanPhamCore.getNgayTao(),
                sanPhamCore.getDanhMuc().getMaDanhMuc(),
                sanPhamCore.getDanhMuc().getTenDanhMuc(),
                sanPhamCore.getThuongHieu().getMaThuongHieu(),
                sanPhamCore.getThuongHieu().getTenThuongHieu(),
                sanPhamDetail == null ? null : sanPhamDetail.getMoTa(),
                sanPhamDetail == null ? null : sanPhamDetail.getThongSoKyThuat()
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }
}
