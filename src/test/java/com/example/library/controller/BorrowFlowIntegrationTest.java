package com.example.library.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flowtest;DB_CLOSE_DELAY=-1",
        "library.seed-data=false"
})
@AutoConfigureMockMvc
class BorrowFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private long createAndGetId(String url, Map<String, Object> body) throws Exception {
        String json = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    @Test
    void borrowThenReturn_updatesAvailableCopies_andBlocksSecondBorrower() throws Exception {
        long bookId = createAndGetId("/api/books", Map.of("isbn", "978-0-1", "title", "Clean Code",
                "author", "Robert Martin", "category", "Programming", "totalCopies", 1));
        long memberA = createAndGetId("/api/members", Map.of("name", "Rahim", "email", "a@example.com"));
        long memberB = createAndGetId("/api/members", Map.of("name", "Karima", "email", "b@example.com"));

        long recordId = createAndGetId("/api/borrowings", Map.of("bookId", bookId, "memberId", memberA));

        mvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(0));

        // The only copy is out, so a second member is refused (422).
        mvc.perform(post("/api/borrowings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bookId", bookId, "memberId", memberB))))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/borrowings/" + recordId + "/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        mvc.perform(get("/api/books/" + bookId))
                .andExpect(jsonPath("$.availableCopies").value(1));

        // Returning again is rejected.
        mvc.perform(post("/api/borrowings/" + recordId + "/return"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createBook_withBlankTitle_returns400_andDuplicateIsbn_returns409() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"978-0-2\",\"title\":\"\",\"author\":\"X\",\"totalCopies\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        Map<String, Object> book = Map.of("isbn", "978-0-3", "title", "T", "author", "A", "totalCopies", 1);
        createAndGetId("/api/books", book);
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isConflict());
    }
}
