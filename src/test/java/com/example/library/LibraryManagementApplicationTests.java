package com.example.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contexttest;DB_CLOSE_DELAY=-1",
        "library.seed-data=false"
})
class LibraryManagementApplicationTests {

    @Test
    void contextLoads() {
    }
}
