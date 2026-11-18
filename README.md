# Library Management System (Spring Boot)

REST API for managing books, members, borrowing/returning and late fines.

**Stack:** Java 17, Spring Boot 3.3, Spring Web, Spring Data JPA (Hibernate), Bean Validation, H2 database, springdoc-openapi (Swagger UI), JUnit 5 + Mockito + MockMvc.

## Run
```
mvn spring-boot:run
```
or run `LibraryManagementApplication` from IntelliJ IDEA.

| URL | Purpose |
|---|---|
| http://localhost:8080/swagger-ui.html | Interactive API docs |
| http://localhost:8080/h2-console | DB console (JDBC URL `jdbc:h2:file:./data/librarydb`, user `sa`, empty password) |

## Business rules (configurable in `application.properties`)
- Loan period: `library.loan-days` (14)
- Max books per member at once: `library.max-active-loans` (3)
- Fine: `library.fine-per-day` (5.00) x days returned after the due date
- A member with unpaid fines cannot borrow; inactive members cannot borrow
- A member cannot borrow the same book twice at the same time
- Book row is locked while borrowing/returning, so the last copy cannot be double-booked

## Endpoints
| Method | Path | Description |
|---|---|---|
| POST/GET | `/api/books` | Create / list+search (`q`, `page`, `size`, `sort`) |
| GET/PUT/DELETE | `/api/books/{id}` | Read / update / delete |
| POST/GET | `/api/members` | Create / list |
| GET/PUT/DELETE | `/api/members/{id}` | Read / update (`active` flag to deactivate) / delete |
| POST | `/api/borrowings` | Borrow `{bookId, memberId}` |
| POST | `/api/borrowings/{id}/return` | Return a book, fine computed |
| POST | `/api/borrowings/{id}/pay-fine` | Mark fine as paid |
| GET | `/api/borrowings?status=BORROWED` | List borrowings |
| GET | `/api/borrowings/overdue` | Overdue books |
| GET | `/api/borrowings/member/{id}` | Member's history |
| GET | `/api/borrowings/member/{id}/fines` | Member's unpaid fines |

Errors are returned as RFC 7807 problem JSON: 400 validation, 404 not found, 409 duplicate, 422 business rule violated.

## Tests
```
mvn test
```

## Switching to MySQL/PostgreSQL
Replace the `h2` dependency in `pom.xml` with the driver and change the `spring.datasource.*` properties.

## Web UI
Open http://localhost:8080 - plain HTML/JS in `src/main/resources/static` (no build step). Tabs: Books, Members, Borrowings.
