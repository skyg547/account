# 지출(Expenditure) & 세무(Tax) 도메인 설계서

> **Issue**: #11
> **브랜치**: `feature/frontend-ui-update`

## 1. 개요
지출관리(Expenditure, Payable, Receivable 포함) 9개 화면과 세무관리(Tax) 4개 화면을 정의하는 설계서입니다. 자금 흐름과 세금계산서의 연동을 시각적으로 구현합니다.

## 2. 화면 목록

### 2.1. 지출관리 (EXPENDITURE)
| 경로 | 화면 구성 및 주요 컴포넌트 |
|---|---|
| `/expenditure/resolution` | 지출결의서 작성 폼, 라인 항목(부서, 비용 계정, 금액), 결재선 지정 |
| `/expenditure/approval` | 지출 승인 워크플로 목록, 결재 대기/완료 탭, 결재 모달 |
| `/expenditure/payable` | 매입채무(AP) 리스트, 지급 예정일, AP 연령 분석 차트 |
| `/expenditure/receivable` | 매출채권(AR) 리스트, 회수 상태, AR 연체 차트 |
| `/expenditure/payment` | 지급 실행(Payment Run). 승인된 AP 기반 지급 리스트, 은행 이체 시뮬레이션 |
| `/expenditure/advance` | 선급금 등록 폼 및 기정산 내역 관리 |
| `/expenditure/collection` | 수금(Collection) 등록, AR 건별 매칭 상계 처리 UI |
| `/expenditure/budget` | 예산 편성/통제 대시보드. 예산 대비 실적(Burn rate) 게이지 바 |
| `/expenditure/cashflow` | 자금 수지 계획표. 입출금 캘린더 및 주간/월간 현금 흐름 추이 차트 |

### 2.2. 세무관리 (TAX)
| 경로 | 화면 구성 및 주요 컴포넌트 |
|---|---|
| `/tax/purchase` | 매입 세금계산서 목록(수취), 국세청 연동 상태 배지 |
| `/tax/sales` | 매출 세금계산서 목록(발행), 역발행 기능 팝업 |
| `/tax/vat` | 부가세 신고 기초 데이터 요약표, 매출/매입 세액 요약 카드 |
| `/tax/nts-verification` | 국세청 승인 번호 검증 그리드, 상태 비교 |

## 3. Mock 데이터 명세 (`src/mocks/expenditure.ts`, `src/mocks/tax.ts`)

### 3.1. 지출 (Expenditure)
- `ResolutionDto`: { id, title, amount, status: 'DRAFT' | 'PENDING' | 'APPROVED', requestor }
- `PayableDto` / `ReceivableDto`: { invoiceId, partnerId, amount, dueDate, status }
- 예산/실적 요약 데이터.

### 3.2. 세무 (Tax)
- `TaxInvoiceDto`: { ntsId, type: 'PURCHASE' | 'SALES', issueDate, supplier, buyer, supplyAmount, vat, totalAmount, status }

## 4. UI 컴포넌트 활용 계획
- **AmountDisplay**: 금액 가독성 제고 (음수, 양수)
- **StatusBadge**: 결재 상태, 세금계산서 상태 표출
- **PageHeader**, **Tabs**, **EmptyState** 
