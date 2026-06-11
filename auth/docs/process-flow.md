# auth process flow

## API 입구

| 메서드 | 경로 | 역할 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | username/password로 로그인하고 JWT를 발급합니다. |
| `POST` | `/api/auth/validate-token-version` | JWT 안의 roleVersion이 현재 사용자 roleVersion과 같은지 확인합니다. |
| `POST` | `/api/auth/internal/users/{username}/role-assignments` | Governance 승인 결과로 사용자 역할 목록을 교체합니다. |

## 로그인 흐름

```mermaid
sequenceDiagram
    participant API as AuthController
    participant Service as AuthService
    participant UserPort as AuthUserQueryPort
    participant Attempt as LoginAttemptPort
    participant Token as TokenIssuerPort

    API->>Service: login(username, password)
    Service->>Attempt: isLocked(username)
    Service->>UserPort: findByUsername(username)
    Service->>Service: password/active/role validity check
    Service->>Attempt: recordSuccess or recordFailure
    Service->>Token: issue(AuthUser)
    Token-->>Service: JWT
    Service-->>API: LoginResponse
```

## 역할 변경 반영 흐름

```mermaid
sequenceDiagram
    participant Gov as Governance
    participant API as AuthController
    participant Service as AuthUserRoleAssignmentService
    participant Port as Persistence Port

    Gov->>API: POST internal role assignments
    Note over Gov,API: X-Internal-Auth-Token 필요
    API->>Service: replaceRoleAssignments
    Service->>Port: replace roles
    Port-->>Service: roleVersion + 1
    Service-->>API: username, roleVersion, roles
```

## Gateway와의 관계

Gateway는 JWT의 서명과 issuer를 검증하고 `X-Auth-User`, `X-Auth-Roles`, `X-Auth-Role-Version`, `X-Auth-Department` 헤더를 뒤쪽 서비스로 전달합니다. 역할 변경 직후 기존 JWT를 강하게 차단해야 하는 경로는 Gateway가 Auth의 token-version 검증 또는 캐시 정책과 연동해야 하며, 이 지점은 Gateway 코드에 `@todo`로 남겼습니다.
