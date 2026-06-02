package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.common.ErrorCodes;
import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.domain.entity.DanhMuc;
import csdlpt.sitemain.domain.entity.SanPhamCore;
import csdlpt.sitemain.domain.entity.SanPhamDetail;
import csdlpt.sitemain.domain.entity.ThuongHieu;
import csdlpt.sitemain.dto.projection.ProductListItemView;
import csdlpt.sitemain.dto.request.ProductUpsertRequest;
import csdlpt.sitemain.dto.response.ProductDetailResponse;
import csdlpt.sitemain.dto.response.ProductListItemResponse;
import csdlpt.sitemain.exception.BusinessException;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.DanhMucRepository;
import csdlpt.sitemain.repository.SanPhamCoreRepository;
import csdlpt.sitemain.repository.SanPhamDetailRepository;
import csdlpt.sitemain.repository.ThuongHieuRepository;
import csdlpt.sitemain.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final SanPhamCoreRepository sanPhamCoreRepository;
    private final SanPhamDetailRepository sanPhamDetailRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;

    public ProductServiceImpl(
            SanPhamCoreRepository sanPhamCoreRepository,
            SanPhamDetailRepository sanPhamDetailRepository,
            DanhMucRepository danhMucRepository,
            ThuongHieuRepository thuongHieuRepository
    ) {
        this.sanPhamCoreRepository = sanPhamCoreRepository;
        this.sanPhamDetailRepository = sanPhamDetailRepository;
        this.danhMucRepository = danhMucRepository;
        this.thuongHieuRepository = thuongHieuRepository;
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

    @Override
    @Transactional
    public ProductDetailResponse createProduct(ProductUpsertRequest request) {
        String maSP = normalizeRequired(request.maSP());
        if (sanPhamCoreRepository.existsById(maSP)) {
            throw validationException("Mã sản phẩm đã tồn tại: " + maSP);
        }

        SanPhamCore sanPhamCore = new SanPhamCore();
        sanPhamCore.setMaSP(maSP);
        applyProductValues(sanPhamCore, request);

        SanPhamCore savedCore = sanPhamCoreRepository.save(sanPhamCore);
        SanPhamDetail savedDetail = upsertProductDetail(savedCore, request);
        return toProductDetailResponse(savedCore, savedDetail);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(String maSP, ProductUpsertRequest request) {
        String pathMaSP = normalizeRequired(maSP);
        String bodyMaSP = normalizeRequired(request.maSP());
        if (!pathMaSP.equals(bodyMaSP)) {
            throw validationException("Mã sản phẩm trên URL và body không khớp");
        }

        SanPhamCore sanPhamCore = sanPhamCoreRepository.findById(pathMaSP)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với mã: " + pathMaSP));
        applyProductValues(sanPhamCore, request);

        SanPhamCore savedCore = sanPhamCoreRepository.save(sanPhamCore);
        SanPhamDetail savedDetail = upsertProductDetail(savedCore, request);
        return toProductDetailResponse(savedCore, savedDetail);
    }

    @Override
    @Transactional
    public void deleteProduct(String maSP) {
        String normalizedMaSP = normalizeRequired(maSP);
        SanPhamCore sanPhamCore = sanPhamCoreRepository.findById(normalizedMaSP)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với mã: " + normalizedMaSP));
        sanPhamCore.setTrangThai(false);
        sanPhamCoreRepository.save(sanPhamCore);
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

    private void applyProductValues(SanPhamCore sanPhamCore, ProductUpsertRequest request) {
        DanhMuc danhMuc = danhMucRepository.findById(normalizeRequired(request.maDanhMuc()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục với mã: " + normalizeRequired(request.maDanhMuc())
                ));
        ThuongHieu thuongHieu = thuongHieuRepository.findById(normalizeRequired(request.maThuongHieu()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với mã: " + normalizeRequired(request.maThuongHieu())
                ));

        sanPhamCore.setTenSP(normalizeRequired(request.tenSP()));
        sanPhamCore.setDanhMuc(danhMuc);
        sanPhamCore.setThuongHieu(thuongHieu);
        sanPhamCore.setGiaBan(request.giaBan());
        sanPhamCore.setDonViTinh(normalizeRequired(request.donViTinh()));
        sanPhamCore.setHinhAnh(normalizeNullable(request.hinhAnh()));
        sanPhamCore.setTrangThai(request.trangThai());
    }

    private SanPhamDetail upsertProductDetail(SanPhamCore sanPhamCore, ProductUpsertRequest request) {
        SanPhamDetail sanPhamDetail = sanPhamDetailRepository.findById(sanPhamCore.getMaSP())
                .orElseGet(SanPhamDetail::new);
        sanPhamDetail.setMaSP(sanPhamCore.getMaSP());
        sanPhamDetail.setSanPhamCore(sanPhamCore);
        sanPhamDetail.setMoTa(normalizeNullable(request.moTa()));
        sanPhamDetail.setThongSoKyThuat(normalizeNullable(request.thongSoKyThuat()));
        return sanPhamDetailRepository.save(sanPhamDetail);
    }

    private BusinessException validationException(String message) {
        return new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
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
