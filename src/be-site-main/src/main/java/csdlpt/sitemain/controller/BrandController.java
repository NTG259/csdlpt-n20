package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.dto.request.BrandUpsertRequest;
import csdlpt.sitemain.dto.response.BrandResponse;
import csdlpt.sitemain.service.BrandService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(brandService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(
            @Valid @RequestBody BrandUpsertRequest request
    ) {
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo thương hiệu thành công", response));
    }

    @PutMapping("/{maThuongHieu}")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable("maThuongHieu") String maThuongHieu,
            @Valid @RequestBody BrandUpsertRequest request
    ) {
        BrandResponse response = brandService.updateBrand(maThuongHieu, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thương hiệu thành công", response));
    }

    @DeleteMapping("/{maThuongHieu}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable("maThuongHieu") String maThuongHieu) {
        brandService.deleteBrand(maThuongHieu);
        return ResponseEntity.ok(ApiResponse.ok("Xóa thương hiệu thành công", null));
    }
}
