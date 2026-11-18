package com.example.library.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Business rules bound from the "library.*" properties. */
@ConfigurationProperties(prefix = "library")
public record LibraryProperties(
        int loanDays,
        int maxActiveLoans,
        BigDecimal finePerDay,
        boolean seedData) {
}
