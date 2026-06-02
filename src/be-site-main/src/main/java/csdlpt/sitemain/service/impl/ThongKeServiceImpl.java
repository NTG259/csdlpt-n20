package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.ThongKeDoanhThuResponse;
import csdlpt.sitemain.repository.ThongKeRepository;
import csdlpt.sitemain.service.ThongKeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ThongKeServiceImpl implements ThongKeService {

    private final ThongKeRepository thongKeRepository;

    public ThongKeServiceImpl(ThongKeRepository thongKeRepository) {
        this.thongKeRepository = thongKeRepository;
    }

    @Override
    public ThongKeDoanhThuResponse thongKeDoanhThu(ThongKeDoanhThuFilter filter) {
        return thongKeRepository.thongKeDoanhThu(filter);
    }
}
