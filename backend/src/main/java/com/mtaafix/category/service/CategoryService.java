package com.mtaafix.category.service;

import com.mtaafix.audit.service.AuditService;
import com.mtaafix.category.dto.CategoryDto;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.user.domain.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final ReportCategoryRepository categoryRepository;
    private final AuditService auditService;

    public CategoryService(ReportCategoryRepository categoryRepository, AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    public List<CategoryDto> listAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryDto::from)
                .collect(Collectors.toList());
    }

    public CategoryDto create(CategoryDto dto, User actor) {
        ReportCategory category = new ReportCategory(
                dto.name(),
                dto.slug(),
                dto.type() != null ? dto.type() : ReportCategory.Type.PRIMARY
        );
        category = categoryRepository.save(category);
        Map<String, String> details = new HashMap<>();
        details.put("name", category.getName());
        details.put("slug", category.getSlug());
        details.put("type", category.getType().name());
        auditService.record("CATEGORY_CREATED", actor, "ReportCategory", category.getId(), details);
        return CategoryDto.from(category);
    }
}
