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

## 4. 재사용 UI 컴포넌트 활용 계획
- **DataTable**: 모든 목록형 화면에 적용 (Pagination, Sorting Mock 기능 포함)
- **StatusBadge**: 계정과목 활성/비활성, 거래처 상태 표시에 적용
- **Tabs**: 거래처 탭 (법인/개인) 및 승인 탭 (대기/완료) 분리에 사용
- **PageHeader**: 일관된 페이지 상단 영역 적용
