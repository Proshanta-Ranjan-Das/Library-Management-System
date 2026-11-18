package com.example.library.service;

import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;
import com.example.library.dto.PageResponse;
import com.example.library.entity.Book;
import com.example.library.exception.BusinessRuleException;
import com.example.library.exception.DuplicateResourceException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public BookService(BookRepository bookRepository, BorrowRecordRepository borrowRecordRepository) {
        this.bookRepository = bookRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        String isbn = request.isbn().trim();
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateResourceException("A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book(isbn, request.title().trim(), request.author().trim(),
                request.category(), request.totalCopies());
        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> search(String query, Pageable pageable) {
        Page<Book> page = (query == null || query.isBlank())
                ? bookRepository.findAll(pageable)
                : bookRepository.search(query.trim(), pageable);
        return PageResponse.from(page.map(BookResponse::from));
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return BookResponse.from(find(id));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        String isbn = request.isbn().trim();
        if (!book.getIsbn().equals(isbn) && bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateResourceException("A book with ISBN " + isbn + " already exists");
        }
        int borrowedNow = book.getTotalCopies() - book.getAvailableCopies();
        if (request.totalCopies() < borrowedNow) {
            throw new BusinessRuleException("Total copies cannot be less than the " + borrowedNow
                    + " copies currently borrowed");
        }
        book.setIsbn(isbn);
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setCategory(request.category());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(request.totalCopies() - borrowedNow);
        return BookResponse.from(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = find(id);
        if (borrowRecordRepository.existsByBookId(id)) {
            throw new BusinessRuleException("Book has borrowing history and cannot be deleted");
        }
        bookRepository.delete(book);
    }

    private Book find(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }
}
