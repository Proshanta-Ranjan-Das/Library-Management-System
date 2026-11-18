package com.example.library.config;

import com.example.library.entity.Book;
import com.example.library.entity.Member;
import com.example.library.repository.BookRepository;
import com.example.library.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Inserts sample data on first run (only when the tables are empty). */
@Component
@ConditionalOnProperty(name = "library.seed-data", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public DataSeeder(BookRepository bookRepository, MemberRepository memberRepository) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            bookRepository.save(new Book("9780132350884", "Clean Code", "Robert C. Martin", "Programming", 3));
            bookRepository.save(new Book("9780134685991", "Effective Java", "Joshua Bloch", "Programming", 2));
            bookRepository.save(new Book("9780262033848", "Introduction to Algorithms", "Thomas H. Cormen", "Algorithms", 2));
            bookRepository.save(new Book("9780201633610", "Design Patterns", "Erich Gamma", "Software Design", 1));
        }
        if (memberRepository.count() == 0) {
            memberRepository.save(new Member("Rahim Uddin", "rahim@example.com", "01700000001"));
            memberRepository.save(new Member("Karima Akter", "karima@example.com", "01800000002"));
        }
    }
}
