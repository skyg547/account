# 기준정보(Master) & 시스템(System) 도메인 설계서

> **Issue**: #8  
> **브랜치**: `feature/frontend-ui-update`

## 1. 개요
재무회계 및 관리회계 전반에서 공통으로 사용되는 핵심 기준정보(계정과목, 거래처, 상품)와 시스템 기반 정보(사용자, 부서, 권한, 감사로그)를 관리하는 화면 구조 및 Mock 데이터 명세입니다.

## 2. 화면 목록

### 2.1. 기준정보 (MASTER)
| 업무 그룹 | 경로 | 화면 구성 및 주요 컴포넌트 |
|---|---|---|
| 계정과목 코드 | `/account-code/tree` | 좌측 5단계 트리뷰, 우측 상세 속성 폼 (Read-only) |
| 계정과목 코드 | `/account-code/manage` | 신규 등록 및 수정 Form, 활성/비활성 Toggle |
| 계정과목 코드 | `/account-code/products` | 금융상품(수신/여신) Data Grid |
| 거래처 관리 | `/partner/list` | 거래처(법인/개인) Data Grid, 상태 뱃지 |
| 거래처 관리 | `/master-data/partner` | 실제 거래처 목록, 신규 등록 요청, 승인/반려 대기열 (Master Data API 연동) |

### 2.2. 시스템 및 내부회계 (SYSTEM)
| 업무 그룹 | 경로 | 화면 구성 및 주요 컴포넌트 |
|---|---|---|
| 시스템관리 | `/system/users` | 사용자 목록 Data Grid, 롤 할당 모달 |
| 시스템관리 | `/system/departments` | 부서/코스트센터 조직도 트리 |
| 시스템관리 | `/system/menus` | Role vs Menu Matrix 테이블 |
| 시스템관리 | `/system/logs` | 시스템 예외 및 배치 에러 로그 뷰어 |
| 내부회계관리 | `/governance/audit-logs` | 시스템 전체 감사 추적 로그 (Read-only Grid) |
| 내부회계관리 | `/governance/rbac` | 권한 그룹 관리 폼 |
| 내부회계관리 | `/governance/controls` | 통제 활동 체크리스트 |

## 3. Mock 데이터 명세

### 3.1. 계정과목 (`AccountSubjectDto`)
- `code`: "1000000" (자산) ~ "5000000" (비용) 체계
- `name`: 계정명 (예: "보통예금")
- `category`: 'GROUP' | 'SUBJECT'
- `status`: 'ACTIVE' | 'INACTIVE'
- `parentCode`: 부모 계정코드

### 3.2. 거래처 (`BusinessPartnerDto`)
- `id`: 거래처 식별키
- `partnerType`: 'CUSTOMER' | 'VENDOR' | 'BANK' | 'OTHER_BP'
- `businessPartnerCode`, `businessPartnerName`: 거래처 코드와 상호명
- `registrationNumber`: API 경계에서 마스킹된 사업자/법인 번호
- `useYn`: 거래처 사용 여부. 승인 대기 상태는 변경 요청의 `REQUESTED`로 분리
- `kycStatus`, `riskRating`, `validFrom`, `validTo`: 심사·위험·SCD2 유효기간

### 3.3. 거래처 변경 요청 (`MasterDataChangeRequestDto`)
- `targetType`: 거래처 화면에서는 `BUSINESS_PARTNER`만 표시
- `status`: 백엔드의 `REQUESTED`를 화면에서 `PENDING`으로 표시하고, 승인/반려 후 `APPROVED`/`REJECTED`로 전이
- `requestedVersion`: 신규 거래처는 첫 SCD2 버전이므로 `1`
- `payloadJson`: 접수·승인 전에 core typed applier가 `BusinessPartnerCommand`/도메인 규칙으로 검증하는 등록 값

### 3.4. 시스템 사용자 (`UserDto`)
- `userId`: 식별키
- `name`: 이름
- `department`: 부서명
- `roles`: UserRole[]
- `lastLoginAt`: 최근 접속일시

### 3.5. `/admin/users` 역할 변경 승인 요청

역할 선택 시 `adminService.requestRoleChange`가 `POST /api/audit/approvals/requests`로 `AUTH_USER_ROLE` 변경 요청을 한 번 보낸다. 현재 백엔드 `MasterApprovalResponse` 계약의 승인 식별자는 `id: Long`이다. 화면은 HTTP 성공과 JSON 파싱만으로 접수를 확정하지 않고, 응답에 유효한 서버 `id`와 승인 상태(`PENDING`, `APPROVED`, `REJECTED`)가 있는지 확인한다. `effectiveDate`는 백엔드에서 nullable이며 날짜가 있을 때만 표시한다. `approvalId`는 현재 백엔드 응답 필드가 아니므로 그것만 있는 응답은 접수 성공으로 취급하지 않는다.

서버가 `PENDING`을 반환하면 선택한 사용자 행에 확인된 승인 ID와 대기 상태를 표시하고 성공 알림을 한 번 띄운다. 다른 상태를 반환하면 그 실제 승인 상태를 알림에 표시하되 사용자 행을 임의로 `PENDING`으로 바꾸지 않는다. 이 화면의 승인 접수 표시는 역할 권한이 즉시 적용됐다는 뜻이 아니다.

HTTP 거절·서버 장애·통신 실패·잘못된 JSON·ID 누락 또는 잘못된 형식·잘못된 승인 상태에서는 오류 알림만 표시한다. 기존 사용자 상태, `pendingRequestId`, 앞서 표시된 승인 성공 메시지와 다른 사용자 행은 유지한다. 서버 오류 상세, 사용자 정보, 토큰은 알림이나 로그에 노출하지 않으며, 자동 재시도로 승인 요청을 중복 생성하지 않는다. 백엔드 경로가 실행 불가능한 환경에서도 로컬 가짜 승인 ID를 만들지 않는다.

로컬 확인은 의존성이 이미 준비된 Node 20/NPM 10 환경의 `frontend` 디렉터리에서 `node --test tests/admin-role-request.test.mjs`, `npx --no-install tsc --noEmit`, `npm run lint -- --quiet` 순으로 실행한다. 테스트는 실제 service와 page 이벤트 경로에 HTTP/통신/JSON/응답 결함을 주입하며 실제 Gateway·승인 DB 접수를 검증하지 않는다.

## 4. 재사용 UI 컴포넌트 활용 계획
- **DataTable**: 모든 목록형 화면에 적용 (Pagination, Sorting Mock 기능 포함)
- **StatusBadge**: 계정과목 활성/비활성, 거래처 상태 표시에 적용
- **Tabs**: 거래처 탭 (법인/개인) 및 승인 탭 (대기/완료) 분리에 사용
- **PageHeader**: 일관된 페이지 상단 영역 적용

## 5. 사이드바 메뉴 권한 조회

`NavProvider`는 현재 UI 역할의 `GET /api/audit/roles/{roleCode}/authorizations`를 동일 출처 BFF로 요청하고 권한 목록과 조회 상태를 별도로 보관합니다. 브라우저는 공개 governance 주소나 JWT를 사용하지 않고, BFF가 HttpOnly 세션을 Gateway 요청에 연결합니다. 역할 선택은 아직 로그인 사용자 역할과 결합되지 않았으므로 서버 권한의 증거로 사용하지 않습니다.

| 입력/상태 | Sidebar 결과 |
| --- | --- |
| 초기 조회, 역할 변경 또는 화면 경로 변경 중 (`loading`) | 이전 조회의 grant를 지우고 권한 대상 메뉴를 숨깁니다. 확인 중 안내를 표시합니다. |
| HTTP 2xx와 올바른 목록 (`success`) | 목록을 그대로 사용합니다. `[]`이면 권한 대상 메뉴가 없으며 연결 성공 상태입니다. |
| `READ`인 명시적 `MENU:/경로` 또는 `MENU:*` | 해당 메뉴 또는 모든 권한 대상 메뉴를 표시합니다. `WRITE`·`EXECUTE`만 있는 항목은 메뉴를 열지 않습니다. |
| 401/403/404, 네트워크/시간 초과, 잘못된 JSON 형태 (`unavailable`) | grant를 지우고 권한 대상 메뉴를 숨깁니다. 조회 실패 안내를 표시합니다. |
| `/login` 화면 | 이전 grant를 즉시 버리고 권한 조회를 하지 않습니다. 로그인 후 다른 화면으로 이동하면 동일한 UI 역할의 권한을 다시 조회합니다. |
| `SYSTEM_ADMIN` | 기존 UI 규칙에 따라 권한 조회 없이 모든 메뉴를 표시합니다. 실제 서버 접근 허용을 뜻하지 않습니다. |

예를 들어 `ACCOUNTING_ADMIN`이 `[{"id":1,"roleCode":"ACCOUNTING_ADMIN","functionCode":"MENU:/ledger/entry","accessType":"READ"}]`을 받으면 전표 입력 링크만 표시됩니다. 같은 역할이 이후 정상 `[]`을 받으면 그 링크는 사라집니다. 응답의 각 항목은 역할 코드와 필수 필드가 올바른지 검사하며, 다른 역할의 응답이나 불완전한 항목은 조회 실패로 처리합니다. 역할·화면 경로 변경이나 조회 실패 시 이전 grant도 재사용하지 않습니다. 같은 화면에 머무는 동안 다른 탭에서 쿠키가 바뀌거나 세션이 만료된 사실은 다음 경로 이동 전까지 감지하지 못합니다. 이때도 실제 서버 요청은 BFF와 Gateway의 인증 검사를 받아야 합니다.

기본 대시보드 `/`와 브랜드 링크, Footer의 시스템 설정 `/system/users`는 Sidebar 권한 대상 메뉴 필터와 별개인 공통 이동 경로입니다. 상단 카테고리 이동과 로그인/로그아웃 UI도 이 목록 필터 밖입니다. 특히 Footer 링크 노출은 `/system/users` 데이터 접근 허용을 뜻하지 않습니다. 이 로직은 프런트 메뉴 노출만 결정하며 서버 접근통제나 실제 로그인 역할 바인딩을 대신하지 않습니다.

`/system/menus` 화면은 역할 목록과 각 비관리자 역할의 권한 목록을 모두 검증한 후에만 표를 표시합니다. 정상 빈 역할 목록은 "등록된 역할이 없습니다"로 표시합니다. 어느 조회라도 실패하거나 응답 형태·역할 코드가 어긋나면 표 전체를 비우고 오류를 표시하며, 로컬 Mock을 대신 사용하지 않습니다. 권한 부여·회수는 동일 출처 BFF를 통해 요청합니다. 서버의 2xx 또는 회수 요청의 `202 Accepted`만으로 저장 완료를 표시하지 않습니다. 쓰기 후 모든 역할을 다시 읽어 대상 권한의 반영을 확인하고, 그대로이거나 재조회에 실패하면 승인 대기·결과 미확인으로 안내합니다. 통신 실패 후에는 자동 재시도나 로컬 토글을 하지 않습니다.

검증 전제는 Node 20/NPM 10과 이미 설치된 `frontend` 의존성입니다. `frontend` 디렉터리에서 `node --test tests/menu-authorizations.test.mjs`, `npx --no-install tsc --noEmit`, `npm run lint -- --quiet`를 실행합니다. Node 테스트는 Provider·Sidebar와 메뉴 화면의 정상/빈 목록/오류/역할 변경/쓰기 후 재조회 흐름을 합성 응답으로 검사합니다. `npm run test:auth-bff`는 세션 쿠키와 프록시의 인증·동일 출처 계약을 별도로 확인합니다. 이 검증은 실제 Gateway route, 실행 서비스 매핑, DB 반영이나 로그인 역할 결합을 입증하지 않습니다.

## 6. 메뉴 권한 연결 계약과 남은 실행 게이트 (#811)

현재 저장소의 확정된 경계는 브라우저 → 동일 출처 `/api` BFF → Gateway → 실행 서비스입니다. BFF는 HttpOnly 세션 쿠키를 읽고 Gateway 요청의 Bearer 헤더를 서버에서 구성합니다. 브라우저가 제출한 신원 헤더는 전달하지 않으며 상태 변경 요청은 동일 출처 조건을 검사합니다. `/api/auth/session`은 현재 인증 여부만 반환합니다. 따라서 `NavContext`의 UI 역할 선택과 `SYSTEM_ADMIN` 표시 규칙은 로그인 사용자의 실제 역할을 증명하지 않습니다. 메뉴 표시나 관리 화면의 버튼은 서버 접근 권한이 아닙니다.

| 계층 | 현재 근거와 책임 | 연결 완료 조건 |
| --- | --- | --- |
| Inbound API / 실행 서비스 | `shared-kernel`의 `AuditController`에 `/api/audit/roles`와 역할별 권한 조회·변경 매핑이 있지만 라이브러리 자체는 실행 호스트가 아닙니다. `internal-audit:api`의 명시적 스캔은 이 웹 패키지를 포함하지 않습니다. #838의 사용자 댓글은 Auth 호스팅을 승인했으며 해당 구현은 별도 진행 중입니다. | 선택된 실행 호스트에서 필요한 Controller, use case, 영속성 어댑터·테이블이 **명시적으로** 조립되고 실제 매핑·인증 테스트가 통과해야 합니다. 다른 서비스에서 `com.ho.account.shared` 전체 스캔으로 우연히 노출시키지 않습니다(#496). |
| Core / persistence | 공유 커널에 `AuthorizationUseCase`, 역할·권한 persistence port/adapter와 repository가 존재합니다. | 권한 변경 시 서버가 인증된 actor, 대상 역할, 승인·감사 기록과 트랜잭션을 검증하고 실행 호스트의 스키마와 연결해야 합니다. 기존 권한 데이터 저장소와 역할 식별자 계약은 아키텍처·보안 책임자가 확정해야 합니다. |
| Gateway | 패키지 설정과 Config Server mirror 모두 `/api/v1/internalaudit/**`는 있지만 `/api/audit/**` route가 없습니다. 별도 #838이 Gateway/Auth/shared-kernel/config-repo 범위를 소유합니다. | 두 설정의 전용 route 대상과 인증 정책이 일치하고, 누락 route는 404, 미인증은 401, 권한 부족은 403으로 구분되는 통합 테스트가 필요합니다. route만 추가해서 완료로 판단하지 않습니다. |
| BFF / Frontend | 메뉴 화면과 Sidebar는 동일 출처 BFF를 통해 읽고 쓰며, 2xx와 올바른 응답만 성공으로 표시합니다. | 로그인 사용자에게 허용된 역할을 신뢰할 수 있는 서버 계약으로 바인딩해야 합니다. 현재 세션의 인증 여부만으로 역할 또는 쓰기 권한을 추론하지 않습니다. 실패·누락·부분 조회에서는 Mock이나 로컬 저장 결과를 권한으로 표시하지 않습니다. |

**정책 결정 대기:** 아키텍처 책임자는 Auth가 최종 governance 실행 owner인지, `/api/audit/**`를 유지할지, 기존 역할·권한 데이터가 어느 DB와 스키마에 있는지 확정해야 합니다. 보안 책임자는 로그인 JWT 역할과 조회 대상 역할의 관계, 조회 허용 범위, 권한 부여·회수의 쓰기 자격, 회수 `202 Accepted`의 승인 대기·감사 표시를 결정해야 합니다. 정책이 정해지기 전에는 프런트 UI 선택 역할이나 `SYSTEM_ADMIN` 버튼 상태를 서버 권한으로 취급하지 않습니다. #838이 진행 중이어도 이 결정과 검증이 완료되었다는 뜻은 아닙니다.

### 분리 작업과 검증 범위

| 작업 / 의존성 | 정확한 변경 범위 또는 보류 사유 | 필수 검증 |
| --- | --- | --- |
| #811 프런트 안전 연동 (이번 Draft) | `frontend/src/context/NavContext.tsx`, `frontend/src/app/system/menus/page.tsx`, `frontend/tests/menu-authorizations.test.mjs`, 필요 시 `frontend/tests/menu-page.test.mjs`, `frontend/docs/master-system-spec.md` (최대 5파일). Gateway·실행 호스트가 준비되기 전에도 실패를 정직하게 표시합니다. | 정상 목록·빈 목록, 401/403/404, 통신 실패·잘못된 JSON·부분 조회, 오래된 응답, POST/DELETE 실패와 재조회 결과, Mock 저장 금지. Node 테스트·TypeScript·lint; `:frontend:test`는 Gradle 프로젝트 존재 여부부터 확인합니다. |
| #838 실행 호스트·Gateway 후속 (별도 작업) | #838이 이미 OPEN/진행 중이며 `gateway/**`, `config-repo/**`, `auth/**`, `shared-kernel/**`를 승인받았습니다. 원래 #811의 자식별 5파일 제한을 넘는 실제 범위이므로 담당 Integrator가 runtime 조립·스키마·보안 정책·Gateway mirror를 충돌 없는 5파일 이하 작업으로 재분할해 정확한 allowlist를 확정해야 합니다. 여기서는 해당 파일을 수정하거나 중복 Issue를 만들지 않습니다. | 합성 DB에서 실제 runtime mapping과 use case/영속성 기동, Gateway 두 설정의 대상·인증 일치, 미인증 401·금지 403·누락 404, 읽기/쓰기 감사·승인 및 회귀 테스트. |
| 최종 연결 검수 (#811과 #838 이후) | 담당자와 allowlist는 두 작업의 계약이 정해진 뒤 확정합니다. 아직 ready 후보가 아닙니다. | local 합성 서버/DB에서 runtime mappings → Gateway → BFF/Frontend의 인증된 조회와 쓰기, 비인가와 실패 상태를 실제 HTTP로 확인합니다. 정적 route 검사나 프런트 단위 테스트만으로 기능 완료를 판정하지 않습니다. |

초보자 확인 순서: 프런트 단위 테스트에서 오류 시 표가 저장 성공을 표시하지 않는지 본 다음, #838이 실행 호스트와 두 Gateway 설정을 검증한 뒤 로그인한 합성 사용자로 읽기와 쓰기를 끝까지 확인합니다. 테스트용 계정·DB만 사용하며 운영 URL, 계정정보, 토큰을 기록하지 않습니다. 이번 문서·프런트 변경의 롤백은 해당 PR 커밋 revert이며, 기존 권한 데이터는 변경하지 않습니다.
