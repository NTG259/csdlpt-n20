package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.common.ErrorCodes;
import csdlpt.sitemain.domain.entity.DanhMuc;
import csdlpt.sitemain.dto.request.CategoryUpsertRequest;
import csdlpt.sitemain.dto.response.CategoryResponse;
import csdlpt.sitemain.exception.BusinessException;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.DanhMucRepository;
import csdlpt.sitemain.service.CategoryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final DanhMucRepository danhMucRepository;

    public CategoryServiceImpl(DanhMucRepository danhMucRepository) {
        this.danhMucRepository = danhMucRepository;
    }

    @Override
    public List<CategoryResponse> getAll() {
        return danhMucRepository.findByTrangThai(true).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryUpsertRequest request) {
        String maDanhMuc = normalizeRequired(request.maDanhMuc());
        if (danhMucRepository.existsById(maDanhMuc)) {
            throw validationException("Mã danh mục đã tồn tại: " + maDanhMuc);
        }

        DanhMuc danhMuc = new DanhMuc();
        danhMuc.setMaDanhMuc(maDanhMuc);
        applyCategoryValues(danhMuc, request);

        return toCategoryResponse(danhMucRepository.save(danhMuc));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(String maDanhMuc, CategoryUpsertRequest request) {
        String pathMaDanhMuc = normalizeRequired(maDanhMuc);
        String bodyMaDanhMuc = normalizeRequired(request.maDanhMuc());
        if (!pathMaDanhMuc.equals(bodyMaDanhMuc)) {
            throw validationException("Mã danh mục trên URL và body không khớp");
        }

        DanhMuc danhMuc = danhMucRepository.findById(pathMaDanhMuc)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với mã: " + pathMaDanhMuc));
        applyCategoryValues(danhMuc, request);

        return toCategoryResponse(danhMucRepository.save(danhMuc));
    }

    @Override
    @Transactional
    public void deleteCategory(String maDanhMuc) {
        String normalizedMaDanhMuc = normalizeRequired(maDanhMuc);
        DanhMuc danhMuc = danhMucRepository.findById(normalizedMaDanhMuc)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục với mã: " + normalizedMaDanhMuc
                ));
        danhMuc.setTrangThai(false);
        danhMucRepository.save(danhMuc);
    }

    private void applyCategoryValues(DanhMuc danhMuc, CategoryUpsertRequest request) {
        String maDanhMuc = normalizeRequired(request.maDanhMuc());
        String maDanhMucCha = normalizeNullable(request.maDanhMucCha());
        if (maDanhMuc.equals(maDanhMucCha)) {
            throw validationException("Danh mục cha không được trùng với danh mục hiện tại");
        }

        DanhMuc danhMucCha = maDanhMucCha == null
                ? null
                : danhMucRepository.findById(maDanhMucCha)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy danh mục cha với mã: " + maDanhMucCha
                        ));

        danhMuc.setTenDanhMuc(normalizeRequired(request.tenDanhMuc()));
        danhMuc.setDanhMucCha(danhMucCha);
        danhMuc.setMoTa(normalizeNullable(request.moTa()));
        danhMuc.setTrangThai(request.trangThai());
    }

    private CategoryResponse toCategoryResponse(DanhMuc danhMuc) {
        return new CategoryResponse(
                danhMuc.getMaDanhMuc(),
                danhMuc.getTenDanhMuc(),
                danhMuc.getDanhMucCha() == null ? null : danhMuc.getDanhMucCha().getMaDanhMuc(),
                danhMuc.getMoTa(),
                danhMuc.getTrangThai()
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

    private BusinessException validationException(String message) {
        return new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
    }
}
