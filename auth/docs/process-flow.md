# auth process flow

## API 입구

| 메서드 | 경로 | 역할 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | 명시적 `NORMAL` 비밀번호 로그인의 자격 증명을 검증하고 JWT를 발급합니다. SSO/LDAP는 신뢰 공급자가 없어 fail-closed입니다. |
| `POST` | `/api/auth/validate-token-version` | 버전과 현재 계정/유효 역할 상태를 확인합니다. |
| `POST` | `/api/auth/internal/users/{username}/role-assignments` | Governance 승인 결과로 사용자 역할 목록을 멱등 교체합니다. |
| `GET` | `/api/admin/users` | 프런트엔드 관리 화면용 사용자 목록을 username 순서로 조회합니다. |

## 관리자 사용자 목록 투영

`GET /api/admin/users`는 Gateway가 JWT에서 다시 만든 `X-Auth-Roles`에 `ROLE_SYSTEM_ADMIN` 또는 `SYSTEM_ADMIN`이 있고 `X-Auth-User`가 비어 있지 않아야 합니다. 헤더 검사에 실패하면 전체 목록을 조회하지 않고 403입니다. core는 헤더의 사용자명으로 현재 저장된 호출자 계정을 다시 조회합니다. 계정이 없거나 비활성·잠금 상태이거나, 저장된 할당 중 미지원 범위가 하나라도 있거나, 현재 유효한 `ROLE_SYSTEM_ADMIN` 또는 `SYSTEM_ADMIN`이 없으면 403입니다. 두 관리자 역할의 대소문자는 구분하지 않으며 `ROLE_ADMIN`만으로는 허용하지 않습니다. 만료·비활성인 미지원 할당도 거부합니다. 통과하면 역할 할당을 함께 읽고 하나의 요청 시각에서 유효 역할을 계산한 뒤 전체 사용자를 username 오름차순으로 반환합니다. 범위·부서별 목록 필터는 이 API에 없습니다. 현재 Auth 스키마의 자연키는 username이며 숫자 ID, 별도 이름·이메일, 마지막 로그인 시각을 저장하지 않으므로 응답은 다음과 같은 제한된 표시용 투영입니다.

- `id`: username의 SHA-256 앞 48비트에 1을 더한 표시용 값입니다. 목록 순서와 무관하게 안정적이고 JavaScript 안전 정수 범위 안이지만, 축약 해시라 충돌할 수 있으며 영속 식별자가 아닙니다.
- `name`, `email`: 모두 username을 사용합니다.
- `role`: 요청 시점에 유효한 첫 역할을 프런트엔드의 `SYSTEM_ADMIN`, `ACCOUNTING_ADMIN`, `RISK_MANAGER`, `RISK_ANALYST`, `MASTER_MANAGER`, `AUDITOR`, `USER` 중 하나로 명시 변환합니다. 레거시 `ROLE_ADMIN`은 `SYSTEM_ADMIN`으로 변환하고, 알 수 없거나 유효한 역할이 없으면 `USER`입니다.
- `status`: 활성 상태이고 잠기지 않은 계정만 `ACTIVE`, 나머지는 `INACTIVE`입니다.
- `lastLogin`: 저장 필드가 없어 빈 문자열입니다.
- `dept`: departmentCode이며 값이 없으면 빈 문자열입니다.

따라서 이 목록의 표시용 ID를 권한 판단이나 사용자 수정·삭제용 키로 사용하면 안 됩니다. 실제 사용자 프로필, 충돌 없는 ID, 로그인 이력 필드가 필요하면 별도 스키마·마이그레이션 계약이 선행되어야 합니다.

## 로그인 흐름

```mermaid
sequenceDiagram
    participant API as AuthController
    participant Service as AuthService
    participant UserPort as AuthUserQueryPort
    participant Attempt as LoginAttemptPort
    participant Domain as AuthUser/RoleAssignment
    participant Token as TokenIssuerPort

    API->>Service: LoginCommand
    Service->>Attempt: isLocked(username)
    Service->>UserPort: findByUsername(username)
    Service->>Service: password/active/admin-lock/department 검사
    Service->>Domain: effectiveRoleAssignmentsAt(authenticatedAt)
    Domain-->>Service: 한 시점의 역할 스냅샷
    Service->>Token: TokenSubject + authenticatedAt
    Token-->>Service: IssuedToken
    Service->>Attempt: recordSuccess
    Service-->>API: AuthenticationResult
    API-->>API: LoginResponse 매핑
```

core는 `api.dto`를 참조하지 않습니다. 역할 유효성 계산 시각을 서비스가 한 번 만들고 JWT 어댑터에 전달하므로 응답과 claim이 같은 스냅샷을 사용합니다.

먼저 모든 저장 역할 할당의 `dataScope`를 확인합니다. JPA에서 읽은 null·공백도 원문 그대로 보존하며, 하나라도 정확한 `GLOBAL`이 아니면 만료·비활성 여부와 관계없이 로그인 실패를 `UNSUPPORTED_ROLE_SCOPE`로 기록하고 JWT 발급 전에 거부합니다. token-version 확인도 이 경우 `valid=false`를 반환합니다. 이전에 서명된 역할의 범위만 만료되고 `GLOBAL` 역할이 남은 경우에도 이전 토큰이 유효해지는 것을 막기 위한 검사입니다. Governance 역할 교체로 미지원 할당을 제거해야 다시 로그인할 수 있습니다. 그 뒤 승인 상태와 `[validFrom, validTo)` 기간으로 유효 역할을 계산합니다.

현재 성공 경로는 대소문자를 구분하지 않는 명시적 `NORMAL`뿐입니다. 신뢰할 SSO 공급자가 없으므로 `SSO` 요청은 일반적인 자격 증명 오류로 끝나며 사용자 조회나 JWT 발급으로 진행하지 않습니다. null, 공백, 알 수 없는 로그인 유형도 같은 공개 오류를 반환합니다. 이들은 자격 증명 검증이 시작되지 않은 요청이므로 로그인 실패 저장소를 조회하거나 실패 횟수와 임시 잠금 카운터를 변경하지 않습니다.

LDAP 요청은 사용자와 비밀번호 및 임시 잠금 정책을 확인한 뒤에도 OTP 검증 공급자가 없으면 항상 일반적인 자격 증명 오류로 끝납니다. 고정값 또는 기본값 OTP를 성공 조건으로 사용하지 않으며 JWT를 발급하지 않습니다.

`AuthService`는 사용자 조회, master-data 원격 확인, 로그인 실패 저장 전체를 하나의 DB 트랜잭션으로 묶지 않습니다. JPA 조회/실패 기록 어댑터가 각각 짧은 read/write 트랜잭션을 소유해 외부 호출 중 DB 연결을 오래 잡거나 실패 기록이 readOnly 경계에 묻히는 일을 막습니다.

### 원격 부서 검증과 로그인 차단

`auth.master-data.enabled=true`이면 `AuthService`가 사용자에게 저장된 부서 코드를
`DepartmentValidationPort`로 전달하고, 원격 어댑터가 `GET /api/basic/departments/{departmentCode}`를 호출합니다.
정확히 HTTP 200이고 JSON 객체의 `code`가 문자열이며 요청 코드와 대소문자·공백까지 일치해야 검증에 성공합니다.
본문 전체가 하나의 JSON 객체여야 하므로 객체 뒤의 공백만 허용하고, 추가 JSON 객체나 잘못된 문자열이 붙으면 거부합니다.
중복 필드도 거부합니다. 특히 `code`가 두 번 나오면 값이 서로 같아도 부서 확인의 증거로 사용하지 않습니다.
`name` 같은 추가 필드는 허용하지만 숫자를 문자열로 바꾸거나 다른 부서의 응답을 허용하지 않습니다.
부서가 없는 사용자에 대한 기존 검사 생략과 명시적 로컬 어댑터의 동작은 그대로입니다.

| 원격 결과 | 로그인 결과 |
| --- | --- |
| 200 + 요청 코드와 같은 문자열 `code`를 가진 객체 | OTP·유효 역할 검사로 진행하고 모든 검사가 성공해야 JWT 발급 |
| 404 | `INVALID_DEPARTMENT` 실패 기록 후 403, `Department code is invalid` |
| 401/403/5xx, 그 밖의 상태, 연결 실패 또는 timeout | 검증 불가 예외로 중단, 현재 API의 5xx 경로 사용 |
| 빈 본문, 읽을 수 없는 JSON, 뒤따르는 추가 데이터, 중복 필드, 객체가 아닌 값, 누락/잘못된 자료형/불일치 `code` | 같은 검증 불가 예외로 중단 |

검증 불가 예외 메시지는 항상 `Department validation is unavailable`이며 원격 예외를 cause나 suppressed로
연결하지 않습니다. API가 예외를 로그에 남겨도 요청 부서, 원격 주소, 응답 본문이 노출되지 않게 하기 위해서입니다.
404 공개 메시지에도 부서 코드를 넣지 않습니다. 검증 장애는 사용자 비밀번호 실패로 집계하지 않으며,
JWT 발급과 로그인 성공 기록에 도달하지 않습니다. 원격 장애를 정상 부서로 간주하는 fallback은 없습니다.

초보자 관점에서는 부서 명부 담당자가 정확한 소속을 확인해 줘야 출입증을 발급하는 절차입니다.
명부를 읽지 못했거나 다른 부서의 답을 받았다면 확인될 때까지 발급을 멈춥니다.
장애 전용 503 매핑은 후속 #800, 연결·응답 대기시간 상한 설정은 #801에서 다룹니다.
현재 timeout 테스트는 예외를 모의한 것이며 실제 대기시간 상한을 검증하지 않습니다.

JDK 17과 캐시된 Gradle 의존성이 있는 저장소 루트에서 다음 회귀 검증을 실행합니다.
`MockRestServiceServer`와 실제 `AuthService`를 연결한 `MockMvc`를 사용하므로 외부 서비스, DB, 실제 credential이 필요 없습니다.

```powershell
./gradlew.bat :auth:core:test :auth:api:test :gateway:test --offline --no-daemon --console=plain --max-workers=2
```

기대 결과는 실패·오류·skip 0입니다. `MasterDataDepartmentValidationAdapterTest`는 상태와 본문 증명을,
`DepartmentValidationLoginIntegrationTest`는 로그인 거부·발급 호출 없음·공개 응답과 애플리케이션 로그의 입력 비노출을 확인합니다.
정상 응답에서는 같은 역할 스냅샷으로 발급 포트가 호출되는지도 확인합니다. 실제 네트워크나 배포 환경의 검증은 포함하지 않습니다.

## 역할 변경 멱등 반영 흐름

Governance 입력의 `dataScope`는 명시적 `GLOBAL`만 허용합니다. null·공백·대소문자 변형·앞뒤 공백·부서 등 다른 범위는 역할 교체와 멱등 기록 전에 거부합니다. 지원 범위를 확장하지 않은 상태에서 승인 값을 임의로 전역 권한으로 넓히지 않기 위해서입니다.

```mermaid
sequenceDiagram
    participant Gov as Governance
    participant API as AuthController
    participant Service as AuthUserRoleAssignmentService
    participant Adapter as JPA/Memory Adapter
    participant User as Auth User Store
    participant Log as Apply Log

    Gov->>API: role assignments + approvalTraceId
    Note over Gov,API: X-Internal-Auth-Token 필요
    API->>Service: ReplaceRoleAssignmentsCommand
    Service->>Service: 역할 정규화 + SHA-256 fingerprint
    Service->>Adapter: RoleAssignmentReplacement
    Adapter->>User: 사용자별 쓰기 lock/조회
    Adapter->>Log: approvalTraceId 조회
    alt 처음 보는 trace
        Adapter->>User: 역할 메타데이터 교체 + roleVersion 증가
        Adapter->>Log: trace/fingerprint/version 저장
    else 같은 trace, 같은 fingerprint
        Adapter-->>Service: 현재 사용자 반환, 재반영 없음
    else 같은 trace, 다른 fingerprint
        Adapter-->>Service: fail-closed 예외
    end
```

JPA 모드는 사용자 행의 비관적 쓰기 lock으로 서로 다른 승인 요청의 버전 증가 순서를 직렬화합니다. apply log는 외부 호출 성공 후 응답 유실로 같은 요청이 재전송되는 상황을 막습니다.

## Token Version 검증

```text
username 존재
AND roleVersion 일치
AND account_active = true
AND account_locked = false
AND 검증 시점의 유효 역할이 1개 이상
AND 저장된 모든 역할 할당(만료·비활성 포함)이 정확한 GLOBAL 범위
=> valid = true
```

## Gateway와의 관계

Gateway는 JWT 서명과 issuer를 검증하고 인증 헤더를 뒤쪽 서비스로 전달합니다. 역할 변경 직후 기존 JWT를 강하게 차단해야 하는 경로는 Auth token-version API를 호출하거나 동일 의미의 캐시 정책을 사용해야 합니다.

이 Auth 변경은 범위 주체, 부서 조건으로 조회를 좁히는 기능이나 master-data의 범위별 쓰기 정책을 제공하지 않습니다. 현재 승인 입력과 JWT 발급은 전역 범위만 통과시키며, 범위별 접근 제어는 별도 서비스 계약이 필요합니다.
