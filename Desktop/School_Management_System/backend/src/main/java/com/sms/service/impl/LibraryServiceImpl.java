package com.sms.service.impl;

import com.sms.entity.BorrowRecord;
import com.sms.entity.LibraryBook;
import com.sms.exception.BadRequestException;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.BorrowRecordRepository;
import com.sms.repository.LibraryBookRepository;
import com.sms.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LibraryServiceImpl implements LibraryService {

    private final LibraryBookRepository libraryBookRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    @Override
    public LibraryBook addBook(LibraryBook book) {
        return libraryBookRepository.save(book);
    }

    @Override
    public LibraryBook updateBook(Long id, LibraryBook book) {
        getBookById(id);
        book.setId(id);
        return libraryBookRepository.save(book);
    }

    @Override
    @Transactional(readOnly = true)
    public LibraryBook getBookById(Long id) {
        return libraryBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LibraryBook> getAllBooks(Pageable pageable) {
        return libraryBookRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LibraryBook> searchBooks(String keyword, Pageable pageable) {
        return libraryBookRepository.searchByTitle(keyword, pageable);
    }

    @Override
    public void deleteBook(Long id) {
        if (!libraryBookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        libraryBookRepository.deleteById(id);
    }

    @Override
    public BorrowRecord borrowBook(BorrowRecord record) {
        LibraryBook book = record.getBook();
        if (book.getAvailableQuantity() <= 0) {
            throw new BadRequestException("No copies available for borrowing");
        }
        book.setAvailableQuantity(book.getAvailableQuantity() - 1);
        libraryBookRepository.save(book);
        return borrowRecordRepository.save(record);
    }

    @Override
    public BorrowRecord returnBook(Long recordId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found"));
        record.setReturnDate(LocalDateTime.now());
        record.setStatus("RETURNED");
        LibraryBook book = record.getBook();
        book.setAvailableQuantity(book.getAvailableQuantity() + 1);
        libraryBookRepository.save(book);
        return borrowRecordRepository.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowRecord> getBorrowRecordsByUser(Long userId) {
        return borrowRecordRepository.findByBorrowerId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowRecord> getActiveBorrows() {
        return borrowRecordRepository.findByStatus("BORROWED");
    }

    public LibraryServiceImpl(LibraryBookRepository libraryBookRepository, BorrowRecordRepository borrowRecordRepository) {
        this.libraryBookRepository = libraryBookRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

}
