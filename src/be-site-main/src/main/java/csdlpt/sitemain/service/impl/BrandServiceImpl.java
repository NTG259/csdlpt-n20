package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.common.ErrorCodes;
import csdlpt.sitemain.domain.entity.ThuongHieu;
import csdlpt.sitemain.dto.request.BrandUpsertRequest;
import csdlpt.sitemain.dto.response.BrandResponse;
import csdlpt.sitemain.exception.BusinessException;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.ThuongHieuRepository;
import csdlpt.sitemain.service.BrandService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final ThuongHieuRepository thuongHieuRepository;

    public BrandServiceImpl(ThuongHieuRepository thuongHieuRepository) {
        this.thuongHieuRepository = thuongHieuRepository;
    }

    @Override
    public List<BrandResponse> getAll() {
        return thuongHieuRepository.findByTrangThai(true).stream()
                .map(this::toBrandResponse)
                .toList();
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandUpsertRequest request) {
        String maThuongHieu = normalizeRequired(request.maThuongHieu());
        String tenThuongHieu = normalizeRequired(request.tenThuongHieu());
        if (thuongHieuRepository.existsById(maThuongHieu)) {
            throw validationException("Mã thương hiệu đã tồn tại: " + maThuongHieu);
        }
        if (thuongHieuRepository.existsByTenThuongHieu(tenThuongHieu)) {
            throw validationException("Tên thương hiệu đã tồn tại: " + tenThuongHieu);
        }

        ThuongHieu thuongHieu = new ThuongHieu();
        thuongHieu.setMaThuongHieu(maThuongHieu);
        applyBrandValues(thuongHieu, request);

        return toBrandResponse(thuongHieuRepository.save(thuongHieu));
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(String maThuongHieu, BrandUpsertRequest request) {
        String pathMaThuongHieu = normalizeRequired(maThuongHieu);
        String bodyMaThuongHieu = normalizeRequired(request.maThuongHieu());
        String tenThuongHieu = normalizeRequired(request.tenThuongHieu());
        if (!pathMaThuongHieu.equals(bodyMaThuongHieu)) {
            throw validationException("Mã thương hiệu trên URL và body không khớp");
        }
        if (thuongHieuRepository.existsByTenThuongHieuAndMaThuongHieuNot(tenThuongHieu, pathMaThuongHieu)) {
            throw validationException("Tên thương hiệu đã tồn tại: " + tenThuongHieu);
        }

        ThuongHieu thuongHieu = thuongHieuRepository.findById(pathMaThuongHieu)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với mã: " + pathMaThuongHieu
                ));
        applyBrandValues(thuongHieu, request);

        return toBrandResponse(thuongHieuRepository.save(thuongHieu));
    }

    @Override
    @Transactional
    public void deleteBrand(String maThuongHieu) {
        String normalizedMaThuongHieu = normalizeRequired(maThuongHieu);
        ThuongHieu thuongHieu = thuongHieuRepository.findById(normalizedMaThuongHieu)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thương hiệu với mã: " + normalizedMaThuongHieu
                ));
        thuongHieu.setTrangThai(false);
        thuongHieuRepository.save(thuongHieu);
    }

    private void applyBrandValues(ThuongHieu thuongHieu, BrandUpsertRequest request) {
        thuongHieu.setTenThuongHieu(normalizeRequired(request.tenThuongHieu()));
        thuongHieu.setTrangThai(request.trangThai());
    }

    private BrandResponse toBrandResponse(ThuongHieu thuongHieu) {
        return new BrandResponse(
                thuongHieu.getMaThuongHieu(),
                thuongHieu.getTenThuongHieu(),
                thuongHieu.getTrangThai()
        );
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private BusinessException validationException(String message) {
        return new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
    }
}
