package com.example.library.dto;

import com.example.library.entity.BorrowStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * For a book that is still out, {@code fineAmount} is the fine accrued so far
 * (it becomes final when the book is returned).
 */
public record BorrowResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        BorrowStatus status,
        boolean overdue,
        BigDecimal fineAmount,
        boolean finePaid) {
}
