package com.example.library.controller;

import com.example.library.dto.BorrowRequest;
import com.example.library.dto.BorrowResponse;
import com.example.library.dto.FineSummaryResponse;
import com.example.library.dto.PageResponse;
import com.example.library.entity.BorrowStatus;
import com.example.library.service.BorrowService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/borrowings")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    /** Borrow a book. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowResponse borrow(@Valid @RequestBody BorrowRequest request) {
        return borrowService.borrow(request);
    }

    /** Return a book; the fine (if late) is calculated here. */
    @PostMapping("/{id}/return")
    public BorrowResponse returnBook(@PathVariable Long id) {
        return borrowService.returnBook(id);
    }

    @PostMapping("/{id}/pay-fine")
    public BorrowResponse payFine(@PathVariable Long id) {
        return borrowService.payFine(id);
    }

    /** GET /api/borrowings?status=BORROWED */
    @GetMapping
    public PageResponse<BorrowResponse> list(@RequestParam(required = false) BorrowStatus status,
                                             @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return borrowService.list(status, pageable);
    }

    @GetMapping("/overdue")
    public List<BorrowResponse> overdue() {
        return borrowService.overdue();
    }

    @GetMapping("/{id}")
    public BorrowResponse get(@PathVariable Long id) {
        return borrowService.get(id);
    }

    @GetMapping("/member/{memberId}")
    public List<BorrowResponse> byMember(@PathVariable Long memberId) {
        return borrowService.byMember(memberId);
    }

    @GetMapping("/member/{memberId}/fines")
    public FineSummaryResponse unpaidFines(@PathVariable Long memberId) {
        return borrowService.unpaidFines(memberId);
    }
}
