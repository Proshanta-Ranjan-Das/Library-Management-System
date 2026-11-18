package com.example.library.service;

import com.example.library.config.LibraryProperties;
import com.example.library.dto.BorrowRequest;
import com.example.library.dto.BorrowResponse;
import com.example.library.dto.FineSummaryResponse;
import com.example.library.dto.PageResponse;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.BorrowStatus;
import com.example.library.entity.Member;
import com.example.library.exception.BusinessRuleException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.MemberRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LibraryProperties properties;
    private final Clock clock;

    public BorrowService(BorrowRecordRepository borrowRepository,
                         BookRepository bookRepository,
                         MemberRepository memberRepository,
                         LibraryProperties properties,
                         Clock clock) {
        this.borrowRepository = borrowRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public BorrowResponse borrow(BorrowRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member", request.memberId()));
        if (!member.isActive()) {
            throw new BusinessRuleException("Member is not active");
        }

        // Lock the book row so concurrent requests cannot both take the last copy.
        Book book = bookRepository.findByIdForUpdate(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", request.bookId()));
        if (book.getAvailableCopies() <= 0) {
            throw new BusinessRuleException("No copies of '" + book.getTitle() + "' are available");
        }
        if (borrowRepository.existsByMemberIdAndBookIdAndStatus(member.getId(), book.getId(), BorrowStatus.BORROWED)) {
            throw new BusinessRuleException("Member already has this book");
        }
        long activeLoans = borrowRepository.countByMemberIdAndStatus(member.getId(), BorrowStatus.BORROWED);
        if (activeLoans >= properties.maxActiveLoans()) {
            throw new BusinessRuleException("Member reached the limit of " + properties.maxActiveLoans()
                    + " borrowed books");
        }
        BigDecimal unpaid = borrowRepository.sumUnpaidFines(member.getId());
        if (unpaid != null && unpaid.signum() > 0) {
            throw new BusinessRuleException("Member has unpaid fines of " + unpaid + "; pay them first");
        }

        LocalDate today = LocalDate.now(clock);
        book.decrementAvailable();
        BorrowRecord record = new BorrowRecord(book, member, today, today.plusDays(properties.loanDays()));
        return toResponse(borrowRepository.save(record));
    }

    @Transactional
    public BorrowResponse returnBook(Long recordId) {
        BorrowRecord record = find(recordId);
        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new BusinessRuleException("This book was already returned");
        }
        Book book = bookRepository.findByIdForUpdate(record.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", record.getBook().getId()));

        LocalDate today = LocalDate.now(clock);
        BigDecimal fine = calculateFine(record.getDueDate(), today);
        record.setReturnDate(today);
        record.setStatus(BorrowStatus.RETURNED);
        record.setFineAmount(fine);
        record.setFinePaid(fine.signum() == 0);
        book.incrementAvailable();
        return toResponse(record);
    }

    @Transactional
    public BorrowResponse payFine(Long recordId) {
        BorrowRecord record = find(recordId);
        if (record.getStatus() != BorrowStatus.RETURNED) {
            throw new BusinessRuleException("Fine can be paid only after the book is returned");
        }
        if (record.getFineAmount().signum() == 0) {
            throw new BusinessRuleException("There is no fine on this record");
        }
        if (record.isFinePaid()) {
            throw new BusinessRuleException("Fine is already paid");
        }
        record.setFinePaid(true);
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public BorrowResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<BorrowResponse> list(BorrowStatus status, Pageable pageable) {
        var page = (status == null)
                ? borrowRepository.findAll(pageable)
                : borrowRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<BorrowResponse> overdue() {
        return borrowRepository.findByStatusAndDueDateBefore(BorrowStatus.BORROWED, LocalDate.now(clock))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowResponse> byMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member", memberId);
        }
        return borrowRepository.findByMemberIdOrderByBorrowDateDesc(memberId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FineSummaryResponse unpaidFines(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member", memberId);
        }
        return new FineSummaryResponse(memberId, borrowRepository.sumUnpaidFines(memberId));
    }

    /** Fine = late days x fine-per-day; zero when returned on or before the due date. */
    public BigDecimal calculateFine(LocalDate dueDate, LocalDate returnedOn) {
        long lateDays = ChronoUnit.DAYS.between(dueDate, returnedOn);
        if (lateDays <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return properties.finePerDay().multiply(BigDecimal.valueOf(lateDays)).setScale(2, RoundingMode.HALF_UP);
    }

    private BorrowRecord find(Long id) {
        return borrowRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Borrow record", id));
    }

    private BorrowResponse toResponse(BorrowRecord r) {
        LocalDate today = LocalDate.now(clock);
        boolean out = r.getStatus() == BorrowStatus.BORROWED;
        boolean overdue = out && today.isAfter(r.getDueDate());
        BigDecimal fine = out ? calculateFine(r.getDueDate(), today) : r.getFineAmount();
        return new BorrowResponse(
                r.getId(),
                r.getBook().getId(), r.getBook().getTitle(),
                r.getMember().getId(), r.getMember().getName(),
                r.getBorrowDate(), r.getDueDate(), r.getReturnDate(),
                r.getStatus(), overdue, fine, r.isFinePaid());
    }
}
