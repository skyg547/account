# master-data beginner guide

`master-data` is the reference-data module for the accounting platform. Other modules rely on it for stable codes and names, such as account subjects, business partners, departments, currencies, fiscal periods, and products.

## Main Concepts

- Account subjects define the chart of accounts and reporting lines.
- Business partners represent customers, vendors, banks, and other counterparties.
- Departments represent organizations, cost centers, and profit centers.
- Currencies and exchange rates support multi-currency accounting.
- Fiscal periods control accounting period status.
- Products provide product or service reference data.
- Change requests add approval and audit control before sensitive master data is applied.

## Current API Groups

- `POST /api/basic/account-subjects`
- `GET /api/basic/account-subjects`
- `GET /api/basic/account-subjects/{code}`
- `PUT /api/basic/account-subjects/{code}`
- `DELETE /api/basic/account-subjects/{code}`
- `POST /api/basic/businesspartners`
- `GET /api/basic/businesspartners`
- `GET /api/basic/businesspartners/active`
- `GET /api/basic/businesspartners/{businessPartnerCode}`
- `GET /api/basic/businesspartners/search?name=...`
- `PUT /api/basic/businesspartners/{id}`
- `DELETE /api/basic/businesspartners/{id}`
- `POST /api/basic/departments`
- `GET /api/basic/departments`
- `GET /api/basic/departments/active`
- `GET /api/basic/departments/{deptCode}`
- `PUT /api/basic/departments/{deptCode}`
- `DELETE /api/basic/departments/{deptCode}`
- `POST /api/basic/products`
- `GET /api/basic/products`
- `GET /api/basic/products/{id}`
- `PUT /api/basic/products/{id}`
- `DELETE /api/basic/products/{id}`
- `POST /api/master-data/change-requests`
- `GET /api/master-data/change-requests/pending`
- `POST /api/master-data/change-requests/{requestId}/approve`
- `POST /api/master-data/change-requests/{requestId}/reject`
- `POST /api/master-data/change-requests/{requestId}/apply`
- `POST /api/master-data/change-requests/apply-due`

## Reading Order

For a typical account-subject request, read the code in this order:

1. `masterdata.api.web.AccountSubjectController`
2. `masterdata.api.dto.AccountSubjectRequestDto`
3. `masterdata.core.application.command.AccountSubjectCommand`
4. `masterdata.core.application.port.in.AccountSubjectUseCase`
5. `masterdata.core.application.service.AccountSubjectService`
6. `masterdata.core.application.port.out.AccountSubjectPersistencePort`
7. `masterdata.core.infrastructure.persistence.JpaAccountSubjectPersistenceAdapter`
8. `masterdata.core.infrastructure.persistence.repository.AccountSubjectRepository`
9. `masterdata.core.domain.model.AccountSubject`

The same pattern applies to business partners, departments, and products.

## Checks Before Changing Code

- Keep controllers and DTOs at the API boundary.
- Keep use-case coordination in application services.
- Keep business state rules in the domain model or domain policy.
- Keep JPA and repository details in infrastructure.
- Prefer validity-period updates over hard deletes when the domain requires auditability.
- Run `./gradlew :master-data:test` after changes.
