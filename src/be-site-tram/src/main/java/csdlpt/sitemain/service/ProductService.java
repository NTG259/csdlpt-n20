package csdlpt.sitemain.service;

import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.response.ProductDetailResponse;
import csdlpt.sitemain.dto.response.ProductListItemResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    PageResponse<ProductListItemResponse> getProducts(
            String maDanhMuc,
            String maThuongHieu,
            Boolean trangThai,
            Pageable pageable
    );

    ProductDetailResponse getProductDetail(String maSP);
}
