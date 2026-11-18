package com.example.library.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Injectable clock so date-based logic (due dates, fines) is testable. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
