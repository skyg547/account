# governance process flow

## 감사 로그 조회 흐름

```mermaid
sequenceDiagram
    participant API as AuditController
    participant Service as AuditService
    participant Port as Audit Persistence Port
    participant DB as JPA Adapter

    API->>Service: find logs
    Service->>Port: query by user/target/type
    Port->>DB: select audit logs
    DB-->>Port: entities
    Port-->>Service: audit logs
    Service-->>API: DTO response
```

감사 로그는 회계 추적성의 출발점입니다. API 응답은 도메인 엔티티를 그대로 노출하지 않고 DTO로 변환합니다.

## 승인 요청과 반영 흐름

```mermaid
flowchart TD
    A[POST /api/audit/approvals/requests] --> B[PENDING 저장]
    B --> C[승인자 검토]
    C --> D{요청자와 승인자가 같은가}
    D -->|Yes| E[SOD 위반 예외]
    D -->|No| F[APPROVED]
    F --> G{masterType 지원 ApplyPort 존재}
    G -->|Yes| H[대상 모듈 반영]
    G -->|No| I[현재는 승인만 저장, @todo fail-closed]
```

`MasterApprovalService`는 승인 후 `MasterDataChangeApplyPort` 구현체를 찾아 대상 모듈에 반영합니다. 지원 포트가 없는 masterType이 조용히 승인되는 문제는 운영 리스크라 코드에 `@todo`로 남겼습니다.

## Auth 역할 승인 흐름

```mermaid
sequenceDiagram
    participant Gov as Governance
    participant Adapter as RestClientAuthUserRoleAssignmentAdapter
    participant Auth as Auth Internal API

    Gov->>Adapter: AUTH_USER_ROLE approval
    Adapter->>Auth: POST /api/auth/internal/users/{username}/role-assignments
    Note over Adapter,Auth: X-Internal-Auth-Token 필요
    Auth-->>Adapter: roleVersion 증가 결과
```

토큰 값은 `GOVERNANCE_AUTH_INTERNAL_TOKEN`과 Auth의 `AUTH_INTERNAL_API_TOKEN`이 같아야 합니다.

## 주요 API

- `GET /api/audit/logs`
- `GET /api/audit/logs/user/{userId}`
- `GET /api/audit/logs/target`
- `GET /api/audit/logs/type/{eventType}`
- `GET /api/audit/roles`
- `POST /api/audit/roles`
- `GET /api/audit/permissions/check`
- `GET /api/audit/approvals/pending`
- `POST /api/audit/approvals/requests`
- `POST /api/audit/approvals/{approvalId}/approve`
- `POST /api/audit/approvals/{approvalId}/reject`
