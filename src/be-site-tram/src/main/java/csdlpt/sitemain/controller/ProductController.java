package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.response.ProductDetailResponse;
import csdlpt.sitemain.dto.response.ProductListItemResponse;
import csdlpt.sitemain.dto.response.TonKhoResponse;
import csdlpt.sitemain.service.ProductService;
import csdlpt.sitemain.service.TonKhoService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final TonKhoService tonKhoService;

    public ProductController(ProductService productService, TonKhoService tonKhoService) {
        this.productService = productService;
        this.tonKhoService = tonKhoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductListItemResponse>>> getProducts(
            @RequestParam(name = "maDanhMuc", required = false) String maDanhMuc,
            @RequestParam(name = "maThuongHieu", required = false) String maThuongHieu,
            @RequestParam(name = "trangThai", required = false) Boolean trangThai,
            @ParameterObject @PageableDefault(size = 10, sort = "ngayTao", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ProductListItemResponse> response = productService.getProducts(
                maDanhMuc,
                maThuongHieu,
                trangThai,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{maSP}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable("maSP") String maSP) {
        ProductDetailResponse response = productService.getProductDetail(maSP);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{maSP}/ton-kho")
    public ResponseEntity<ApiResponse<TonKhoResponse>> getTonKho(@PathVariable("maSP") String maSP) {
        return ResponseEntity.ok(ApiResponse.ok(tonKhoService.kiemTraTonKho(maSP)));
    }
}
