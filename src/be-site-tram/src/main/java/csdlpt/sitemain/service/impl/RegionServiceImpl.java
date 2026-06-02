package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.dto.response.RegionResponse;
import csdlpt.sitemain.repository.KhuVucRepository;
import csdlpt.sitemain.service.RegionService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RegionServiceImpl implements RegionService {

    private final KhuVucRepository khuVucRepository;

    public RegionServiceImpl(KhuVucRepository khuVucRepository) {
        this.khuVucRepository = khuVucRepository;
    }

    @Override
    public List<RegionResponse> getAll() {
        return khuVucRepository.findAll().stream()
                .map(khuVuc -> new RegionResponse(
                        khuVuc.getMaKhuVuc(),
                        khuVuc.getTenKhuVuc()
                ))
                .toList();
    }
}
