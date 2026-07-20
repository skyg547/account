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
| approvalTraceId | Governance 승인 한 건을 식별하는 멱등 키. 같은 승인 재시도를 구분합니다. |
| request fingerprint | 사용자·역할·기간·승인자를 정규화해 만든 SHA-256 값. 같은 trace의 내용 변경을 탐지합니다. |
| internal API token | Governance가 Auth 내부 API를 호출할 때 사용하는 공유 토큰. |

## 로그인 데이터 흐름

1. API가 `LoginRequest`를 검증합니다.
2. Controller가 core `LoginCommand`로 변환합니다.
3. `AuthService`가 임시 잠금, 사용자, 비밀번호, 활성 상태, 관리 잠금, 부서, 역할 순으로 검사합니다.
4. 공용 `Clock`의 한 시점에서 승인되고 유효한 역할을 확정합니다.
5. 같은 역할 목록으로 JWT와 core `AuthenticationResult`를 만듭니다.
6. Controller가 core 결과를 `LoginResponse`로 변환합니다.

이렇게 하면 역할 만료 경계에서 응답에는 역할이 있지만 JWT에는 없거나, 그 반대가 되는 문제를 막을 수 있습니다.

## Governance 역할 승인 데이터 흐름

1. Governance가 승인 ID를 포함한 `approvalTraceId`로 내부 API를 호출합니다.
2. Auth는 역할 코드를 대문자 `ROLE_` 형식으로 정규화하고 요청 fingerprint를 만듭니다.
3. 저장 어댑터가 같은 trace의 적용 이력을 확인합니다.
4. 처음 보는 요청이면 역할 메타데이터를 보존해 교체하고 `roleVersion`을 증가시킵니다.
5. 같은 요청의 재시도면 아무 것도 다시 바꾸지 않습니다.
6. 같은 trace에 다른 내용이 오면 오류로 중단합니다.

## 운영에서 중요한 점

- 비활성·관리 잠금·유효 역할 없음 계정은 token-version 검증도 실패합니다.
- memory 모드는 단일 프로세스용이며, 운영 다중 인스턴스는 JPA 로그인 실패 저장소를 사용합니다.
- 레거시 평문 비밀번호, 최초 실패 동시 insert, 멱등 이력 보존 정책은 코드 `@todo`로 추적합니다.
- Auth의 `AUTH_INTERNAL_API_TOKEN`과 Governance의 `GOVERNANCE_AUTH_INTERNAL_TOKEN`은 같은 값을 사용해야 합니다.
