package com.example.library.repository;

import com.example.library.entity.BorrowRecord;
import com.example.library.entity.BorrowStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    long countByMemberIdAndStatus(Long memberId, BorrowStatus status);

    boolean existsByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, BorrowStatus status);

    boolean existsByBookId(Long bookId);

    boolean existsByMemberId(Long memberId);

    @Override
    @EntityGraph(attributePaths = {"book", "member"})
    Page<BorrowRecord> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"book", "member"})
    Page<BorrowRecord> findByStatus(BorrowStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"book", "member"})
    List<BorrowRecord> findByMemberIdOrderByBorrowDateDesc(Long memberId);

    @EntityGraph(attributePaths = {"book", "member"})
    List<BorrowRecord> findByStatusAndDueDateBefore(BorrowStatus status, LocalDate date);

    @Query("""
            select coalesce(sum(r.fineAmount), 0) from BorrowRecord r
            where r.member.id = :memberId and r.fineAmount > 0 and r.finePaid = false
            """)
    BigDecimal sumUnpaidFines(@Param("memberId") Long memberId);
}
