package com.mtaafix.category.service;

import com.mtaafix.category.dto.CategoryDto;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.repository.ReportCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final ReportCategoryRepository categoryRepository;

    public CategoryService(ReportCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto> listAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryDto::from)
                .collect(Collectors.toList());
    }

    public CategoryDto create(CategoryDto dto) {
        ReportCategory category = new ReportCategory(
                dto.name(),
                dto.slug(),
                dto.type() != null ? dto.type() : ReportCategory.Type.PRIMARY
        );
        category = categoryRepository.save(category);
        return CategoryDto.from(category);
    }
}
