package com.example.library.dto;

import java.math.BigDecimal;

public record FineSummaryResponse(Long memberId, BigDecimal unpaidFines) {
}
