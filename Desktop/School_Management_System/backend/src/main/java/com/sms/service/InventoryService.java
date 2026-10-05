package com.sms.service;

import com.sms.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {
    Inventory addItem(Inventory item);

    Inventory updateItem(Long id, Inventory item);

    Inventory getItemById(Long id);

    Page<Inventory> getAllItems(Pageable pageable);

    Page<Inventory> searchItems(String keyword, Pageable pageable);

    void deleteItem(Long id);

    List<Inventory> getByCategory(String category);

    List<Inventory> getByStatus(String status);

    Inventory adjustQuantity(Long id, int delta);
}
