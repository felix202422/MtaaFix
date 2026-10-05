package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.Inventory;
import com.sms.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Inventory>>> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Inventory> items = inventoryService.getAllItems(pageable);
        PagedResponse<Inventory> response = PagedResponse.<Inventory>builder()
                .content(items.getContent()).page(items.getNumber()).size(items.getSize())
                .totalElements(items.getTotalElements()).totalPages(items.getTotalPages())
                .first(items.isFirst()).last(items.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<Inventory>>> searchItems(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Inventory> items = inventoryService.searchItems(q, pageable);
        PagedResponse<Inventory> response = PagedResponse.<Inventory>builder()
                .content(items.getContent()).page(items.getNumber()).size(items.getSize())
                .totalElements(items.getTotalElements()).totalPages(items.getTotalPages())
                .first(items.isFirst()).last(items.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Inventory>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getItemById(id)));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Inventory>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getByCategory(category)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Inventory>>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getByStatus(status)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<Inventory>> addItem(@Valid @RequestBody Inventory item) {
        return ResponseEntity.ok(ApiResponse.success("Item added", inventoryService.addItem(item)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<Inventory>> updateItem(@PathVariable Long id, @Valid @RequestBody Inventory item) {
        return ResponseEntity.ok(ApiResponse.success("Item updated", inventoryService.updateItem(id, item)));
    }

    @PatchMapping("/{id}/quantity")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<Inventory>> adjustQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        int delta = body.getOrDefault("delta", 0);
        return ResponseEntity.ok(ApiResponse.success("Quantity adjusted", inventoryService.adjustQuantity(id, delta)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        inventoryService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success("Item deleted", null));
    }

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

}
