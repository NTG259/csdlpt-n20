package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.response.RegionResponse;
import java.util.List;

public interface RegionService {

    List<RegionResponse> getAll();
}
