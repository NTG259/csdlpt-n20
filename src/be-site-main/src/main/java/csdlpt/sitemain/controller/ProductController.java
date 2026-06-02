package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.request.ProductUpsertRequest;
import csdlpt.sitemain.dto.response.ProductDetailResponse;
import csdlpt.sitemain.dto.response.ProductListItemResponse;
import csdlpt.sitemain.dto.response.TonKhoHeThongResponse;
import csdlpt.sitemain.service.ProductService;
import csdlpt.sitemain.service.TonKhoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody ProductUpsertRequest request
    ) {
        ProductDetailResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo sản phẩm thành công", response));
    }

    @PutMapping("/{maSP}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable("maSP") String maSP,
            @Valid @RequestBody ProductUpsertRequest request
    ) {
        ProductDetailResponse response = productService.updateProduct(maSP, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật sản phẩm thành công", response));
    }

    @DeleteMapping("/{maSP}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("maSP") String maSP) {
        productService.deleteProduct(maSP);
        return ResponseEntity.ok(ApiResponse.ok("Xóa sản phẩm thành công", null));
    }

    @Operation(summary = "Tồn kho toàn hệ thống của sản phẩm")
    @GetMapping("/{maSP}/ton-kho")
    public ResponseEntity<ApiResponse<TonKhoHeThongResponse>> getTonKhoHeThong(
            @PathVariable("maSP") String maSP
    ) {
        TonKhoHeThongResponse response = tonKhoService.getTonKhoToanHeThong(maSP);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
