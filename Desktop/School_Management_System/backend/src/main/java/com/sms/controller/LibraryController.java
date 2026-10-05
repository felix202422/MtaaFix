package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.BorrowRecord;
import com.sms.entity.LibraryBook;
import com.sms.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @GetMapping("/books")
    public ResponseEntity<ApiResponse<PagedResponse<LibraryBook>>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LibraryBook> books = libraryService.getAllBooks(pageable);
        PagedResponse<LibraryBook> response = PagedResponse.<LibraryBook>builder()
                .content(books.getContent()).page(books.getNumber()).size(books.getSize())
                .totalElements(books.getTotalElements()).totalPages(books.getTotalPages())
                .first(books.isFirst()).last(books.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/books/search")
    public ResponseEntity<ApiResponse<PagedResponse<LibraryBook>>> searchBooks(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        org.springframework.data.domain.Page<LibraryBook> books = libraryService.searchBooks(q, pageable);
        PagedResponse<LibraryBook> response = PagedResponse.<LibraryBook>builder()
                .content(books.getContent()).page(books.getNumber()).size(books.getSize())
                .totalElements(books.getTotalElements()).totalPages(books.getTotalPages())
                .first(books.isFirst()).last(books.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/books")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<LibraryBook>> addBook(@Valid @RequestBody LibraryBook book) {
        return ResponseEntity.ok(ApiResponse.success("Book added", libraryService.addBook(book)));
    }

    @PutMapping("/books/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<LibraryBook>> updateBook(@PathVariable Long id, @Valid @RequestBody LibraryBook book) {
        return ResponseEntity.ok(ApiResponse.success("Book updated", libraryService.updateBook(id, book)));
    }

    @DeleteMapping("/books/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        libraryService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success("Book deleted", null));
    }

    @PostMapping("/borrow")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<BorrowRecord>> borrowBook(@Valid @RequestBody BorrowRecord record) {
        return ResponseEntity.ok(ApiResponse.success("Book borrowed", libraryService.borrowBook(record)));
    }

    @PutMapping("/return/{recordId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<BorrowRecord>> returnBook(@PathVariable Long recordId) {
        return ResponseEntity.ok(ApiResponse.success("Book returned", libraryService.returnBook(recordId)));
    }

    @GetMapping("/borrows/active")
    public ResponseEntity<ApiResponse<java.util.List<BorrowRecord>>> getActiveBorrows() {
        return ResponseEntity.ok(ApiResponse.success(libraryService.getActiveBorrows()));
    }

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

}
