package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.request.CategoryUpsertRequest;
import csdlpt.sitemain.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAll();

    CategoryResponse createCategory(CategoryUpsertRequest request);

    CategoryResponse updateCategory(String maDanhMuc, CategoryUpsertRequest request);

    void deleteCategory(String maDanhMuc);
}
