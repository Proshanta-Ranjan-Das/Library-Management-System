package com.example.library.service;

import com.example.library.dto.MemberRequest;
import com.example.library.dto.MemberResponse;
import com.example.library.dto.PageResponse;
import com.example.library.entity.Member;
import com.example.library.exception.BusinessRuleException;
import com.example.library.exception.DuplicateResourceException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.MemberRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public MemberService(MemberRepository memberRepository, BorrowRecordRepository borrowRecordRepository) {
        this.memberRepository = memberRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        String email = request.email().trim().toLowerCase();
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A member with email " + email + " already exists");
        }
        Member member = new Member(request.name().trim(), email, request.phone());
        if (request.active() != null) {
            member.setActive(request.active());
        }
        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> list(Pageable pageable) {
        return PageResponse.from(memberRepository.findAll(pageable).map(MemberResponse::from));
    }

    @Transactional(readOnly = true)
    public MemberResponse get(Long id) {
        return MemberResponse.from(find(id));
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Member member = find(id);
        String email = request.email().trim().toLowerCase();
        if (!member.getEmail().equals(email) && memberRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A member with email " + email + " already exists");
        }
        member.setName(request.name().trim());
        member.setEmail(email);
        member.setPhone(request.phone());
        if (request.active() != null) {
            member.setActive(request.active());
        }
        return MemberResponse.from(member);
    }

    @Transactional
    public void delete(Long id) {
        Member member = find(id);
        if (borrowRecordRepository.existsByMemberId(id)) {
            throw new BusinessRuleException(
                    "Member has borrowing history and cannot be deleted; deactivate the member instead");
        }
        memberRepository.delete(member);
    }

    private Member find(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Member", id));
    }
}
