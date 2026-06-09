# Interaction Summary

This document summarizes the current understanding of the `account` project based on our initial setup.

## Project Overview

The `account` project is a Spring Boot application designed as an accounting system. It uses a layered architecture to manage various accounting entities such as `AccountSubject`, `Customer`, `Department`, `JournalEntry`, `FixedAsset`, `Budget`, and `TaxInvoice`. A key convention is the implementation of Slowly Changing Dimension Type 2 (SCD2) for entities like `AccountSubject` to track historical data via validity periods.

## Technologies Used

*   **Language:** Java 17
*   **Framework:** Spring Boot (3.2.5)
*   **Build System:** Gradle
*   **Web:** Spring Web (for REST APIs)
*   **Data Persistence:** Spring Data JPA
*   **Database:** H2 Database (in-memory, for development/testing)
*   **Testing:** JUnit 5

## Key Development Conventions

*   **API Style:** RESTful with DTOs.
*   **Architecture:** Layered (controllers, services, repositories).
*   **Data Access:** Spring Data JPA.
*   **Transaction Management:** `@Transactional` on service methods.
*   **SCD2:** Implemented for master data like `AccountSubject`.
*   **Code Structure:** Organized by feature domain.

## Build and Run Commands

*   **Build:** `./gradlew clean build`
*   **Run:** `./gradlew bootRun` or `java -jar build/libs/account-0.0.1-SNAPSHOT.jar`
*   **Test:** `./gradlew test`