package com.example.library.dto;

import com.example.library.entity.Book;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String category,
        int totalCopies,
        int availableCopies) {

    public static BookResponse from(Book b) {
        return new BookResponse(b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(),
                b.getCategory(), b.getTotalCopies(), b.getAvailableCopies());
    }
}
