# 여신(Loan) & 공정가치(Fair Value) 도메인 설계서

> **Issue**: #12
> **브랜치**: `feature/frontend-ui-update`

## 1. 개요
이연대출부대손익(Loan/Deposit) 관리 6개 화면과 IFRS 16 리스를 포함한 공정가치(Asset/Lease) 관리 7개 화면을 정의하는 설계서입니다.

## 2. 화면 목록

### 2.1. 여신/수신/이연 (LOAN / DEPOSIT)
| 경로 | 화면 구성 및 주요 컴포넌트 |
|---|---|
| `/loan/contracts` | 대출 계약 목록, 금리(고정/변동), 대출 기간 등 계약 원장 뷰 |
| `/loan/disbursal` | 대출 지급 실행 리스트, 회계 전표 자동 발행 현황 |
| `/loan/deferred` | 이연 수수료(부대수익)/원가(부대비용) 항목 등록 폼 |
| `/loan/amortization` | 유효이자율(EIR) 방식의 상각 스케줄 표, 회차별 이자수익/상각액 |
| `/loan/events` | 중도상환, 조건변경 등 이벤트 처리 및 EIR 재계산 트리거 |
| `/loan/deposit` | 예금 계좌 개설, 입출금 거래 내역, 수신 이자 스케줄 |

### 2.2. 공정가치 / 리스 (FAIR VALUE / LEASE)
| 경로 | 화면 구성 및 주요 컴포넌트 |
|---|---|
| `/fair-value/assets` | 고정자산(유형/무형) 대장 리스트, 취득원가, 내용연수 |
| `/fair-value/depreciation` | 정액법/정률법 감가상각 시뮬레이션 및 월배치 실행 결과 |
| `/fair-value/disposal` | 자산 매각/폐기 처리, 처분 손익 계산 요약 |
| `/fair-value/revaluation` | 자산 재평가 모델, 손상차손(Impairment) 테스트 결과 입력 폼 |
| `/fair-value/lease` | IFRS 16 기준 리스 계약 등록, 사용권자산(ROU) 및 리스부채 현재가치 계산 |
| `/fair-value/lease-monthly` | 리스 월결산. 이자비용 인식 및 감가상각 분개 내역 |
| `/fair-value/lease-remeasure` | 리스료 변경(물가지수 연동 등)에 따른 부채 재측정 이력 |

## 3. Mock 데이터 명세 (`src/mocks/loan.ts`, `src/mocks/fair-value.ts`)

### 3.1. 여신 (Loan)
- `LoanContractDto`: { contractId, borrower, principal, interestRate, maturityDate, eir }
- `AmortizationScheduleDto`: { seq, date, payment, interest, principal, balance }

### 3.2. 공정가치 (Fair Value)
- `AssetDto`: { assetId, name, category, acquisitionCost, bookValue, lifeYears }
- `LeaseContractDto`: { leaseId, lessor, presentValue, interestRate, termMonths }

## 4. UI 컴포넌트 활용 계획
- **PageHeader**, **Tabs**, **StatusBadge** (계약 상태, 상각 상태)
- **AmountDisplay**: 현재가치, 취득원가 표기
