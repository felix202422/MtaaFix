package com.sms.repository;

import com.sms.entity.LibraryBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {

    Optional<LibraryBook> findByIsbn(String isbn);

    Optional<LibraryBook> findByBarcode(String barcode);

    List<LibraryBook> findByCategory(String category);

    @Query("SELECT b FROM LibraryBook b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :text, '%'))")
    org.springframework.data.domain.Page<LibraryBook> searchByTitle(@Param("text") String text, org.springframework.data.domain.Pageable pageable);
}
