package com.sms.service.impl;

import com.sms.entity.Inventory;
import com.sms.exception.BadRequestException;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.InventoryRepository;
import com.sms.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public Inventory addItem(Inventory item) {
        if (item.getStatus() == null) {
            item.setStatus("AVAILABLE");
        }
        if (item.getQuantity() == null) {
            item.setQuantity(0);
        }
        return inventoryRepository.save(item);
    }

    @Override
    public Inventory updateItem(Long id, Inventory item) {
        getItemById(id);
        item.setId(id);
        return inventoryRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Inventory getItemById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inventory> getAllItems(Pageable pageable) {
        return inventoryRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Inventory> searchItems(String keyword, Pageable pageable) {
        return inventoryRepository.searchByName(keyword, pageable);
    }

    @Override
    public void deleteItem(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory item not found with id: " + id);
        }
        inventoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> getByCategory(String category) {
        return inventoryRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> getByStatus(String status) {
        return inventoryRepository.findByStatus(status);
    }

    @Override
    public Inventory adjustQuantity(Long id, int delta) {
        Inventory item = getItemById(id);
        int newQuantity = item.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new BadRequestException("Quantity cannot be negative");
        }
        item.setQuantity(newQuantity);
        return inventoryRepository.save(item);
    }

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

}
