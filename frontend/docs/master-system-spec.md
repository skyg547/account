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
