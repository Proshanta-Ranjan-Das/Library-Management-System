package com.example.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.library.config.LibraryProperties;
import com.example.library.dto.BorrowRequest;
import com.example.library.dto.BorrowResponse;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.BorrowStatus;
import com.example.library.entity.Member;
import com.example.library.exception.BusinessRuleException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock BorrowRecordRepository borrowRepository;
    @Mock BookRepository bookRepository;
    @Mock MemberRepository memberRepository;

    private final LibraryProperties props = new LibraryProperties(14, 3, new BigDecimal("5.00"), false);

    private BorrowService serviceOn(String isoDate) {
        Clock clock = Clock.fixed(Instant.parse(isoDate + "T10:00:00Z"), ZoneOffset.UTC);
        return new BorrowService(borrowRepository, bookRepository, memberRepository, props, clock);
    }

    private Book bookWithId(long id, int total, int available) {
        Book book = new Book("111", "Clean Code", "Robert Martin", "Programming", total);
        book.setAvailableCopies(available);
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private Member memberWithId(long id) {
        Member member = new Member("Rahim", "rahim@example.com", "017");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    void fine_isZero_whenReturnedOnDueDate() {
        BorrowService service = serviceOn("2026-01-20");
        assertEquals(0, service.calculateFine(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 20))
                .compareTo(BigDecimal.ZERO));
    }

    @Test
    void fine_isZero_whenReturnedEarly() {
        BorrowService service = serviceOn("2026-01-20");
        assertEquals(0, service.calculateFine(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 10))
                .compareTo(BigDecimal.ZERO));
    }

    @Test
    void fine_isDaysLateTimesRate() {
        BorrowService service = serviceOn("2026-01-20");
        assertEquals(new BigDecimal("15.00"),
                service.calculateFine(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 23)));
    }

    @Test
    void returnBook_late_setsFine_andRestocksBook() {
        Book book = bookWithId(1L, 1, 0);
        Member member = memberWithId(2L);
        BorrowRecord record = new BorrowRecord(book, member, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15));
        Mockito.when(borrowRepository.findById(10L)).thenReturn(Optional.of(record));
        Mockito.when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(book));

        BorrowResponse response = serviceOn("2026-01-18").returnBook(10L); // 3 days late

        assertEquals(BorrowStatus.RETURNED, response.status());
        assertEquals(new BigDecimal("15.00"), response.fineAmount());
        assertEquals(false, response.finePaid());
        assertEquals(1, book.getAvailableCopies());
    }

    @Test
    void returnBook_twice_isRejected() {
        Book book = bookWithId(1L, 1, 1);
        BorrowRecord record = new BorrowRecord(book, memberWithId(2L), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15));
        record.setStatus(BorrowStatus.RETURNED);
        Mockito.when(borrowRepository.findById(10L)).thenReturn(Optional.of(record));

        assertThrows(BusinessRuleException.class, () -> serviceOn("2026-01-18").returnBook(10L));
    }

    @Test
    void borrow_whenNoCopiesLeft_isRejected() {
        Mockito.when(memberRepository.findById(2L)).thenReturn(Optional.of(memberWithId(2L)));
        Mockito.when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bookWithId(1L, 1, 0)));

        assertThrows(BusinessRuleException.class,
                () -> serviceOn("2026-01-18").borrow(new BorrowRequest(1L, 2L)));
    }

    @Test
    void borrow_whenMemberHasUnpaidFines_isRejected() {
        Mockito.when(memberRepository.findById(2L)).thenReturn(Optional.of(memberWithId(2L)));
        Mockito.when(bookRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bookWithId(1L, 2, 2)));
        Mockito.when(borrowRepository.sumUnpaidFines(2L)).thenReturn(new BigDecimal("10.00"));

        assertThrows(BusinessRuleException.class,
                () -> serviceOn("2026-01-18").borrow(new BorrowRequest(1L, 2L)));
    }
}
