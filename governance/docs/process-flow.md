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
    D -->|No| F{masterType 지원 ApplyPort 존재}
    F -->|No| G[fail-closed 예외]
    F -->|Yes| H[APPROVED]
    H --> I{Master Data 미래 시행일인가}
    I -->|No| J[대상 모듈 즉시 반영]
    I -->|Yes| K[Master Data APPROVED 예약 대기]
```

`MasterApprovalService`는 승인 상태로 전이하기 전에 `MasterDataChangeApplyPort` 구현체를 찾습니다. 지원 포트가 없는 `masterType`은 조용히 승인하지 않고 fail-closed 예외로 중단합니다. 즉시 반영 대상에서 "결재 도장은 찍었는데 실제 기준정보는 안 바뀐" 상태를 만들지 않기 위한 안전장치입니다. 미래 시행일의 예약 대기는 승인된 날짜 정책에 따른 의도적인 상태입니다.

Master Data 연동은 Governance `approvalId`를 `sourceReference`로 전달합니다. 동일 승인 재시도는
기존 변경 요청의 상태를 이어가며, 이미 반영됐으면 아무 작업도 반복하지 않습니다. 미래
`effectiveDate`는 Master Data의 `APPROVED` 상태로 저장되고 `/apply-due` 경로가 시행일 도래 후 반영합니다.

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
## 역할/권한 승인 반영 표

| masterType | requestType | 실제 반영 | 정책 |
| --- | --- | --- | --- |
| `SYSTEM_ROLE` | `CREATE` | 역할 저장 | 지원 |
| `SYSTEM_ROLE` | `UPDATE`, `DELETE` | 반영하지 않음 | fail-closed 예외 |
| `AUTHORIZATION` | `CREATE` | 역할·충돌·중복 확인 후 권한 저장 | 지원 |
| `AUTHORIZATION` | `DELETE` | 승인 payload의 권한 ID로 삭제 | 지원 |
| `AUTHORIZATION` | `UPDATE` | 반영하지 않음 | fail-closed 예외 |

## 권한 회수 데이터 흐름

```mermaid
sequenceDiagram
    participant Client as API 호출자
    participant API as AuditController
    participant Service as AuditService
    participant Approval as MasterApprovalService
    participant Adapter as SystemRoleApprovalApplyAdapter
    participant DB as Governance DB

    Client->>API: DELETE /authorizations/{id}
    API->>Service: revokeAuthorization(id)
    Service->>DB: 현재 권한과 역할 조회
    Service->>Approval: AUTHORIZATION DELETE 승인 요청
    Approval->>DB: PENDING 저장
    API-->>Client: 202 + approvalId/status
    Client->>Approval: 다른 사용자가 approve
    Approval->>Adapter: 승인 변경 반영
    Adapter->>DB: authorization deleteById
    Approval->>DB: APPROVED 저장
```

`DELETE` API 호출 시점에는 권한이 유지됩니다. 승인 완료 시점에만 실제 삭제되므로, 요청자는 반환된 승인 ID로 처리 상태를 추적해야 합니다.
