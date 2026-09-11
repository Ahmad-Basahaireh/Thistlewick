# Thistlewick

> A personal task & reminder manager — backend core engine.

**Thistlewick** is a Java 17 command-line application that manages users,
tasks, and reminders on top of an embedded H2 database using plain JDBC.
It was built as the Month-1 capstone of a Java backend internship.

---

## Project Status

🚧 **Under active development** — see the [Commit Plan](#commit-plan) below.

---

## Tech Stack

| Layer            | Choice                          | Why                                                       |
|------------------|---------------------------------|-----------------------------------------------------------|
| Language         | Java 17                         | LTS, records, sealed types, modern Stream API             |
| Build tool       | Maven                           | Dependency management + standardized layout               |
| Database         | H2 (embedded, file mode)        | Zero-setup, runs anywhere                                 |
| Persistence      | Plain JDBC                      | Full control over SQL; no ORM magic                       |
| Testing          | JUnit 5                         | TDD for domain + algorithm + service layers               |
| Interface        | Terminal CLI                    | Keeps focus on backend engineering                        |

---

## Build Tool Choice: Why Maven

Maven is used **only as a build and dependency management tool**.
All application logic is hand-written:

- JDBC connections, `PreparedStatement`s, `ResultSet` mapping — written from scratch.
- Custom sorting algorithm — hand-rolled (no `Collections.sort`).
- Observer pattern — custom `EventBus` implementation.
- `ReminderFactory` — manual factory pattern.
- Streams aggregations — pure Java 17 Stream API.

Maven's role is limited to:

- Fetching `h2.jar` and `junit.jar` from Maven Central.
- Standardizing the `src/main/java` + `src/test/java` layout.
- Providing `mvn compile`, `mvn test`, `mvn exec:java`.

Without Maven, the same code would require manual jar downloads and
classpath management — which adds **no learning value** and only risks
environment-specific failures. The learning objectives (OOP, JDBC,
algorithms, patterns, concurrency) are fully preserved.

---

## Setup & Run

### Prerequisites
- JDK 17 or higher
- Maven 3.8+

### Build
```bash
mvn clean compile