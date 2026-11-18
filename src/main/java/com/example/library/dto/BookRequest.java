package com.example.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank(message = "ISBN is required") @Size(max = 20, message = "ISBN must be at most 20 characters") String isbn,
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Author is required") String author,
        String category,
        @Min(value = 1, message = "Total copies must be at least 1") int totalCopies) {
}
