package com.mtaafix.category.controller;

import com.mtaafix.common.response.SimpleResponse;
import com.mtaafix.category.dto.CategoryDto;
import com.mtaafix.category.dto.CategoryOrderRequest;
import com.mtaafix.category.service.CategoryService;
import com.mtaafix.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Categories", description = "Report category endpoints")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Get active categories (picker list; admins may include inactive)")
    @GetMapping
    public ResponseEntity<List<CategoryDto>> list(
            @Parameter(description = "Include deactivated categories (ADMINISTRATOR only)")
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal User user) {
        if (includeInactive) {
            if (user == null || user.getRole() != User.Role.ADMINISTRATOR) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Only administrators can list deactivated categories.");
            }
            return ResponseEntity.ok(categoryService.list(false));
        }
        return ResponseEntity.ok(categoryService.listAll());
    }

    @Operation(summary = "Get active categories (public, unauthenticated filter list for FR-8.3)")
    @GetMapping("/public")
    public ResponseEntity<List<CategoryDto>> listPublic() {
        return ResponseEntity.ok(categoryService.listAll());
    }

    @Operation(summary = "Create a new category (FR-5.1, admin only, audited)")
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<CategoryDto> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.create(dto, user));
    }

    @Operation(summary = "Update a category (FR-5.1, admin only, audited)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<CategoryDto> update(@PathVariable String id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.update(id, dto, user));
    }

    @Operation(summary = "Activate or deactivate a category (FR-5.1, audited)")
    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<CategoryDto> setActive(@PathVariable String id,
            @RequestParam boolean value,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categoryService.setActive(id, value, user));
    }

    @Operation(summary = "Set a category's display order (FR-5.1, audited)")
    @PutMapping("/{id}/order")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<CategoryDto> setOrder(@PathVariable String id,
            @Valid @RequestBody CategoryOrderRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categoryService.setOrder(id, request.displayOrder(), user));
    }
}
