package com.sms.repository;

import com.sms.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByCategory(String category);

    List<Inventory> findByStatus(String status);

    @Query("SELECT i FROM Inventory i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.category) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(i.supplier) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Inventory> searchByName(@Param("keyword") String keyword, Pageable pageable);
}
