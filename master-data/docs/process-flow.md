# master-data process flow

## Standard Master Data Request

```mermaid
flowchart LR
    A[REST Controller] --> B[Request DTO]
    B --> C[Command]
    C --> D[UseCase Input Port]
    D --> E[Application Service]
    E --> F[Output Port]
    F --> G[JPA Persistence Adapter]
    G --> H[Spring Data Repository]
    H --> I[(master-data DB)]
```

Application services coordinate the use case and transaction. They call output ports and domain objects, not Spring Data repositories directly.

## Change Request Flow

```mermaid
stateDiagram-v2
    [*] --> REQUESTED : requestChange
    REQUESTED --> APPROVED : approve by different user
    REQUESTED --> REJECTED : reject
    APPROVED --> APPLIED : apply
```

```mermaid
flowchart LR
    A[Requester] --> B[MasterDataChangeRequestController]
    B --> C[MasterDataChangeRequestUseCase]
    C --> D[MasterDataChangeRequestService]
    D --> E[MasterDataChangeRequestPersistencePort]
    E --> F[JpaMasterDataChangeRequestPersistenceAdapter]
    F --> G[(master_data_change_requests)]
```

Important rules:

- The requester and approver must be different users.
- Only `REQUESTED` changes can be approved or rejected.
- Only `APPROVED` changes can be applied.
- `apply-due` applies only approved requests whose effective date is today or earlier.
- `payloadJson` is converted into the target command by `DefaultMasterDataChangeApplier`.

## Lookup Flow For Other Modules

```mermaid
flowchart LR
    A[journal-ledger] --> D[MasterDataQueryPort]
    B[closing] --> D
    C[other modules] --> D
    D --> E[MonolithMasterDataQueryAdapter]
    E --> F[masterdata repositories]
```

Other modules should depend on lookup contracts, not on master-data JPA entities.
