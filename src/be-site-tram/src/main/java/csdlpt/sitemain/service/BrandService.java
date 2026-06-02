package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.response.BrandResponse;
import java.util.List;

public interface BrandService {

    List<BrandResponse> getAll();
}
