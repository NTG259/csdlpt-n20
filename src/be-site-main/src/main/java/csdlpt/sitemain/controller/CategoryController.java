package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.dto.request.CategoryUpsertRequest;
import csdlpt.sitemain.dto.response.CategoryResponse;
import csdlpt.sitemain.service.CategoryService;
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
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryUpsertRequest request
    ) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo danh mục thành công", response));
    }

    @PutMapping("/{maDanhMuc}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable("maDanhMuc") String maDanhMuc,
            @Valid @RequestBody CategoryUpsertRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(maDanhMuc, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật danh mục thành công", response));
    }

    @DeleteMapping("/{maDanhMuc}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable("maDanhMuc") String maDanhMuc) {
        categoryService.deleteCategory(maDanhMuc);
        return ResponseEntity.ok(ApiResponse.ok("Xóa danh mục thành công", null));
    }
}
