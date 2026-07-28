# 대손충당금(ECL) & 마트/대사(Mart) 도메인 설계서

> **Issue**: #13
> **브랜치**: `feature/frontend-ui-update`

## 1. 개요
대손충당금(Expected Credit Loss, ECL) 산출 및 검증을 위한 5개 화면과, 금융 마트데이터 및 데이터 대사(Reconciliation) 처리를 위한 8개 화면을 정의하는 설계서입니다.

## 2. 화면 목록

### 2.1. 대손충당금 (ECL)
| 경로 | 화면 구성 및 주요 컴포넌트 |
|---|---|
| `/ecl/batch` | ECL 배치 관제탑. 월말 배치 실행 프로그레스 바, 실시간 로그 |
| `/ecl/exposures` | 여신 익스포저(Exposure) 데이터 추출 결과 및 분류(Stage 1,2,3) |
| `/ecl/parameters` | ECL 모델 파라미터(PD, LGD, CCF) 설정 및 변경 이력 |
| `/ecl/results` | ECL 산출 결과. 계약별/포트폴리오별 충당금 산출액(Recharts 파이/바 차트) |
| `/ecl/ead` | 부도 시 익스포저(EAD) 엔진 산출 로직 검증 및 시뮬레이션 패널 |

### 2.2. 마트데이터 관리 및 대사 (MART & RECONCILIATION)
| 경로 | 화면 구성 및 주요 컴포넌트 |
|---|---|
| `/mart/explorer` | 마트 탐색기. 계정계/정보계 데이터 테이블 스키마 뷰어 및 쿼리 프리뷰 |
| `/mart/dq-audit` | 데이터 품질(DQ) 감사 대시보드. Null 비율, 정합성 오류 현황 |
| `/mart/exchange-rates` | 환율 관리. 고시 환율 달력, 통화별 환율 추이(Recharts 라인 차트) |
| `/mart/market-rates` | 시장금리 관리. LIBOR, CD, 국고채 등 금리 인덱스 조회 |
| `/mart/yield-curves` | 수익률곡선(Yield Curve). 만기별 이자율 곡선 그래프 (Recharts) |
| `/mart/reconciliation-setup` | 대사 규칙 설정. 원장(GL)과 마트 데이터간 비교 Key 매핑 룰 정의 |
| `/mart/reconciliation-run` | 대사 실행. 대사 배치 트리거 및 성공/실패율 프로그레스 링 |
| `/mart/reconciliation-diff` | 차이 해소. 대사 불일치(Diff) 항목 리스트, 원인 분석 메모 폼 |

## 3. Mock 데이터 명세 (`src/mocks/ecl.ts`, `src/mocks/mart.ts`)

### 3.1. ECL
- `EclResultDto`: { portfolio, stage1, stage2, stage3, totalEcl }
- `EclParameterDto`: { type, value, validFrom, validTo }

### 3.2. Mart
- `ExchangeRateDto`: { currency, date, baseRate, buyRate, sellRate }
- `YieldCurveDto`: { term, rate }
- `ReconciliationDiffDto`: { id, source, target, diffAmount, status }

## 4. UI 컴포넌트 활용 계획
- **PageHeader**, **Tabs**, **StatusBadge** 
- **Recharts**: 수익률 곡선, 환율 추이, ECL 비중 시각화
