package com.example.library.dto;

import com.example.library.entity.Member;
import java.time.LocalDate;

public record MemberResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate membershipDate,
        boolean active) {

    public static MemberResponse from(Member m) {
        return new MemberResponse(m.getId(), m.getName(), m.getEmail(), m.getPhone(),
                m.getMembershipDate(), m.isActive());
    }
}
