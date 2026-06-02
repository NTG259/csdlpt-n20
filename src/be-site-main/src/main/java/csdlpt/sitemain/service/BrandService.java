package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.request.BrandUpsertRequest;
import csdlpt.sitemain.dto.response.BrandResponse;
import java.util.List;

public interface BrandService {

    List<BrandResponse> getAll();

    BrandResponse createBrand(BrandUpsertRequest request);

    BrandResponse updateBrand(String maThuongHieu, BrandUpsertRequest request);

    void deleteBrand(String maThuongHieu);
}
