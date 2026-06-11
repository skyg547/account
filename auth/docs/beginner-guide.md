# auth beginner guide

## 한 문장 요약

`auth`는 사용자가 한 번 로그인하면 다른 서비스에서도 쓸 수 있는 전자 출입증, 즉 JWT를 발급합니다.

초보자 관점에서는 회사 출입증 발급 데스크입니다. 출입증 안에는 사용자 이름, 역할, 부서 코드, 역할 버전이 들어갑니다.

## 핵심 용어

| 용어 | 설명 |
| --- | --- |
| JWT | 로그인 성공 후 발급되는 서명된 토큰. |
| role | 사용자가 가진 권한 묶음. 예: `ROLE_ADMIN`. |
| dataScope | 권한이 적용되는 데이터 범위. |
| roleVersion | 역할이 바뀔 때 증가하는 숫자. 기존 토큰 재사용을 막는 기준. |
| internal API token | Governance가 Auth 내부 API를 호출할 때 사용하는 공유 토큰. |

## 큰 흐름

1. 사용자가 `/api/auth/login`에 username/password를 보냅니다.
2. `AuthService`가 사용자를 조회하고 비밀번호와 잠금 상태를 확인합니다.
3. 성공하면 승인되고 유효기간이 맞는 역할만 골라 JWT를 발급합니다.
4. 역할이 바뀌면 `roleVersion`이 증가합니다.
5. 오래된 JWT는 `/api/auth/validate-token-version`에서 현재 roleVersion과 맞지 않아 invalid가 됩니다.

## 운영에서 중요한 점

- 기본 로그인 실패 잠금 어댑터는 메모리 기반입니다. 서버가 여러 대면 잠금 상태가 서버별로 갈라질 수 있어 Redis/DB 어댑터가 필요합니다.
- Governance가 사용자 역할을 승인하면 Auth 내부 API가 기존 역할 목록을 승인된 목록으로 교체합니다.
- Auth의 `AUTH_INTERNAL_API_TOKEN`과 Governance의 `GOVERNANCE_AUTH_INTERNAL_TOKEN`은 같은 값이어야 합니다.
