package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.dto.response.CategoryResponse;
import csdlpt.sitemain.repository.DanhMucRepository;
import csdlpt.sitemain.service.CategoryService;
import java.util.List;
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
                .map(danhMuc -> new CategoryResponse(
                        danhMuc.getMaDanhMuc(),
                        danhMuc.getTenDanhMuc(),
                        danhMuc.getDanhMucCha() == null ? null : danhMuc.getDanhMucCha().getMaDanhMuc(),
                        danhMuc.getMoTa(),
                        danhMuc.getTrangThai()
                ))
                .toList();
    }
}
