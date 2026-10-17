package com.mtaafix.category.service;

import com.mtaafix.audit.service.AuditService;
import com.mtaafix.category.dto.CategoryDto;
import com.mtaafix.common.exceptions.ResourceNotFoundException;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.user.domain.User;
import org.springframework.data.domain.Sort;
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

    /** Active categories only (default list for pickers, FR-5.1 deactivation). */
    public List<CategoryDto> listAll() {
        return list(true);
    }

    /**
     * @param activeOnly true = only active categories; false = every category
     *                   (administrators only — enforced by the controller).
     */
    public List<CategoryDto> list(boolean activeOnly) {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder")
                        .and(Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .filter(c -> !activeOnly || c.isActive())
                .map(CategoryDto::from)
                .collect(Collectors.toList());
    }

    public CategoryDto create(CategoryDto dto, User actor) {
        if (categoryRepository.findBySlug(dto.slug().trim()).isPresent()) {
            throw new IllegalStateException("Category slug already exists: " + dto.slug());
        }
        ReportCategory category = new ReportCategory(
                dto.name().trim(),
                dto.slug().trim(),
                dto.type() != null ? dto.type() : ReportCategory.Type.PRIMARY
        );
        if (dto.parentId() != null && !dto.parentId().isBlank()) {
            category.setParentId(dto.parentId().trim());
        }
        if (dto.active() != null) {
            category.setActive(dto.active());
        }
        if (dto.displayOrder() != null) {
            category.setDisplayOrder(dto.displayOrder());
        }
        category = categoryRepository.save(category);
        Map<String, String> details = new HashMap<>();
        details.put("name", category.getName());
        details.put("slug", category.getSlug());
        details.put("type", category.getType().name());
        auditService.record("CATEGORY_CREATED", actor, "ReportCategory", category.getId(), details);
        return CategoryDto.from(category);
    }

    public CategoryDto update(String id, CategoryDto dto, User actor) {
        ReportCategory category = load(id);
        if (!category.getSlug().equals(dto.slug().trim())
                && categoryRepository.findBySlug(dto.slug().trim()).isPresent()) {
            throw new IllegalStateException("Category slug already exists: " + dto.slug());
        }
        category.setName(dto.name().trim());
        category.setSlug(dto.slug().trim());
        if (dto.type() != null) {
            category.setType(dto.type());
        }
        category.setParentId(dto.parentId() != null && !dto.parentId().isBlank()
                ? dto.parentId().trim() : null);
        if (dto.active() != null) {
            category.setActive(dto.active());
        }
        if (dto.displayOrder() != null) {
            category.setDisplayOrder(dto.displayOrder());
        }
        category = categoryRepository.save(category);
        auditService.record("CATEGORY_UPDATED", actor, "ReportCategory", category.getId(),
                Map.of("slug", category.getSlug()));
        return CategoryDto.from(category);
    }

    public CategoryDto setActive(String id, boolean active, User actor) {
        ReportCategory category = load(id);
        category.setActive(active);
        category = categoryRepository.save(category);
        auditService.record(active ? "CATEGORY_ACTIVATED" : "CATEGORY_DEACTIVATED", actor,
                "ReportCategory", category.getId(), Map.of("slug", category.getSlug()));
        return CategoryDto.from(category);
    }

    public CategoryDto setOrder(String id, Integer displayOrder, User actor) {
        if (displayOrder == null) {
            throw new IllegalArgumentException("displayOrder is required.");
        }
        ReportCategory category = load(id);
        category.setDisplayOrder(displayOrder);
        category = categoryRepository.save(category);
        auditService.record("CATEGORY_REORDERED", actor, "ReportCategory", category.getId(),
                Map.of("slug", category.getSlug(), "displayOrder", String.valueOf(displayOrder)));
        return CategoryDto.from(category);
    }

    private ReportCategory load(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
