package com.sms.service;

import com.sms.entity.BorrowRecord;
import com.sms.entity.LibraryBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface LibraryService {
    LibraryBook addBook(LibraryBook book);
    LibraryBook updateBook(Long id, LibraryBook book);
    LibraryBook getBookById(Long id);
    Page<LibraryBook> getAllBooks(Pageable pageable);
    Page<LibraryBook> searchBooks(String keyword, Pageable pageable);
    void deleteBook(Long id);
    BorrowRecord borrowBook(BorrowRecord record);
    BorrowRecord returnBook(Long recordId);
    List<BorrowRecord> getBorrowRecordsByUser(Long userId);
    List<BorrowRecord> getActiveBorrows();
}
