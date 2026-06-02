package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.dto.response.BrandResponse;
import csdlpt.sitemain.repository.ThuongHieuRepository;
import csdlpt.sitemain.service.BrandService;
import java.util.List;
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
                .map(thuongHieu -> new BrandResponse(
                        thuongHieu.getMaThuongHieu(),
                        thuongHieu.getTenThuongHieu(),
                        thuongHieu.getTrangThai()
                ))
                .toList();
    }
}
