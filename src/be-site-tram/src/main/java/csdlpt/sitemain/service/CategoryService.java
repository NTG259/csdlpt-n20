package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAll();
}
