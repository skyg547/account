# Project: account

## Project Overview

This project is a Spring Boot application named `account` that provides an accounting system. It uses Spring Web for building REST APIs, Spring Data JPA for data persistence, and an H2 database for development and testing. The application manages various accounting entities such as `AccountSubject`, `Customer`, `Department`, `JournalEntry`, `FixedAsset`, `Budget`, and `TaxInvoice`. It follows a layered architecture with controllers handling web requests, services encapsulating business logic, and repositories managing data interactions. A notable convention is the use of Slowly Changing Dimension Type 2 (SCD2) for entities like `AccountSubject` to track validity periods.

## Technologies Used

*   **Language:** Java 17
*   **Framework:** Spring Boot (3.2.5)
*   **Build System:** Gradle
*   **Web:** Spring Web
*   **Data Persistence:** Spring Data JPA
*   **Database:** H2 Database (in-memory for development/testing)
*   **Testing:** JUnit 5

## Building and Running

To build, run, and test the project, use the following Gradle commands:

*   **Build the project:**
    ```bash
    ./gradlew clean build
    ```
*   **Run the application:**
    ```bash
    ./gradlew bootRun
    ```
    Alternatively, after building, you can run the generated JAR file:
    ```bash
    java -jar build/libs/account-0.0.1-SNAPSHOT.jar
    ```
*   **Run tests:**
    ```bash
    ./gradlew test
    ```

## Development Conventions

*   **Language Version:** Java 17 is used for development.
*   **API Style:** RESTful API design with data transfer objects (DTOs) for request and response payloads.
*   **Layered Architecture:** Clear separation of concerns into web (controllers), service (business logic), and data (repositories) layers.
*   **Data Access:** Spring Data JPA repositories are used for simplified data access.
*   **Transaction Management:** `@Transactional` annotation is consistently applied to service methods to define transactional boundaries.
*   **Slowly Changing Dimensions (SCD2):** For master data like `AccountSubject`, validity periods (`validFrom`, `validTo`) are managed to implement SCD2 principles, allowing for historical tracking of changes.
*   **Code Structure:** Packages are organized by feature domain (e.g., `basic`, `asset`, `journal`, `expenditure`).

## Gemini Role In This Repository

Gemini's default role is independent review, not implementation.

*   Codex is the primary owner for implementation, test fixes, documentation updates, and final verification.
*   Gemini reviews Codex changes and reports defects, regressions, missing tests, architectural violations, and documentation mismatches.
*   Gemini should not modify code unless the user explicitly asks Gemini to implement.
*   Gemini should use the root `GEMINI_REVIEW_PROMPT.md` as the handoff prompt for review tasks.
*   Review output should list findings first, ordered by severity, with file and line references.
