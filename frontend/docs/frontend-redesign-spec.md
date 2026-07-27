# 은행 재무회계 프론트엔드 업무 설계서

> **Issue**: #TBD (프론트엔드 전면 재설계)  
> **브랜치**: `feature/frontend-ui-update`  
> **기술 스택**: Next.js 15 (App Router) · React 19 · TypeScript 5.9 · Tailwind CSS 4 · Recharts  
> **접근 방식**: Mock 데이터 우선 → UI/UX 검증 → 백엔드 API 연동 (후속)

---

## 설계 원칙

1. **디렉터리 = 백엔드 모듈 경계**: Next.js App Router의 디렉터리 기반 라우팅을 활용하여 백엔드 모듈과 1:1 매핑
2. **Mock 우선**: 모든 화면을 Mock 데이터로 먼저 구현하고 UI/UX를 검증한 뒤 API 연동은 별도 Phase
3. **대메뉴 = 업무 기능**: 13개 대메뉴 카테고리, 7개 TopHeader 카테고리
4. **메뉴 정의 모듈화**: `src/components/layout/menus/` 에 백엔드 모듈별 메뉴 파일 분리
5. **기존 디자인 시스템 유지**: Glassmorphism + Deep Navy Dark Mode + 기존 코드 패턴 준수

---

## 1. TopHeader 카테고리 (7개)

| # | 탭 ID | 한글명 | 포함 업무 영역 |
|---|---|---|---|
| 1 | `DASHBOARD` | 대시보드 | 통합 재무 현황 |
| 2 | `ACCOUNTING` | 재무회계 | 원장관리, 결산관리, 보고서관리 |
| 3 | `OPERATIONS` | 자금운영 | 지출관리, 세무관리 |
| 4 | `CREDIT` | 여신·자산 | 이연대출부대손익관리, 공정가치관리 |
| 5 | `RISK` | 리스크·데이터 | 대손충당금관리, 마트데이터관리 및 대사 |
| 6 | `MASTER` | 기준정보 | 계정과목코드관리, 거래처관리 |
| 7 | `SYSTEM` | 시스템관리 | 내부회계관리, 시스템관리(사용자/부서) |

---

## 2. 사이드바 메뉴 그룹 (13개) × 백엔드 모듈 매핑

| # | 메뉴 그룹 | 백엔드 모듈 | TopHeader | 메뉴 파일 |
|---|---|---|---|---|
| 1 | 대시보드 | (전체 집계) | DASHBOARD | `menus/dashboard.ts` |
| 2 | 원장관리 | `journal-ledger` | ACCOUNTING | `menus/ledger.ts` |
| 3 | 결산관리 | `closing` | ACCOUNTING | `menus/closing.ts` |
| 4 | 보고서관리 | `reporting` | ACCOUNTING | `menus/reports.ts` |
| 5 | 지출관리 | `expenditure-resolution` + `payable` + `receivable` | OPERATIONS | `menus/expenditure.ts` |
| 6 | 세무관리 | `tax` | OPERATIONS | `menus/tax.ts` |
| 7 | 이연·대출·부대손익관리 | `loan` + `deposit` | CREDIT | `menus/loan.ts` |
| 8 | 공정가치관리 | `asset-lease` | CREDIT | `menus/fair-value.ts` |
| 9 | 대손충당금관리 | `ecl` | RISK | `menus/ecl.ts` |
| 10 | 마트데이터관리 및 대사 | `account-mart` + `reconciliation` | RISK | `menus/mart.ts` |
| 11 | 계정과목코드관리 | `master-data` | MASTER | `menus/account-code.ts` |
| 12 | 거래처관리 | `master-data` | MASTER | `menus/partner.ts` |
| 13 | 내부회계관리 | `governance` | SYSTEM | `menus/governance.ts` |
| 14 | 시스템관리 | (admin) | SYSTEM | `menus/system.ts` |

---

## 3. 전체 화면 목록 (69개)

### 3-1. 대시보드 (DASHBOARD) — `dashboard`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 1 | 통합 재무 현황 | `/` | 총자산/부채/순이익 KPI, 전표 유입 피드, 볼륨 차트 |

### 3-2. 원장관리 (ACCOUNTING) — `journal-ledger`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 2 | 전표 입력 | `/ledger/entry` | 차/대 행 입력, 대차 검증, 계정 팝업 검색 |
| 3 | 전표 조회 | `/ledger/list` | 기간/상태별 전표 목록, 상세 조회, 승인/전기 |
| 4 | 총계정원장 (GL) | `/ledger/gl` | 계정별 기간 합계, 잔액 조회, 드릴다운 |
| 5 | 보조원장 (SL) | `/ledger/sl` | 거래처별/부서별 잔액, 거래 내역 |
| 6 | 자동 분개 규칙 | `/ledger/rules` | 이벤트→전표 자동 생성 규칙 설정 |
| 7 | 미결항목 정리 | `/ledger/unsettled` | 거래처별 미결항목 조회, 수동 정리 |

### 3-3. 결산관리 (ACCOUNTING) — `closing`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 8 | 결산 캘린더 | `/closing/calendar` | 월별 결산 일정, 마일스톤 타임라인, DoD 평가 |
| 9 | 결산 태스크 | `/closing/tasks` | 단계별 체크리스트, 담당자 배정, 상태 추적 |
| 10 | 게이트 점검 | `/closing/gates` | 결산 전 필수 조건 충족 판정 |
| 11 | 기간 잠금/재개 | `/closing/period-lock` | 회계 기간 잠금, 재개 승인 요청/처리 |
| 12 | 외화 평가 배치 | `/closing/valuation` | FX 환산/평가 배치 실행 모니터링 |
| 13 | 결산 조정 전표 | `/closing/adjustment` | 결산 조정 분개 등록/조회 |
| 14 | 연차 결산 | `/closing/annual` | 손익 대체(이익잉여금 전환) 실행 |

### 3-4. 보고서관리 (ACCOUNTING) — `reporting`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 15 | 재무제표 조회 | `/reports/statements` | BS/IS 동적 생성, 인터랙티브 뷰어 |
| 16 | 보고서 내보내기 | `/reports/export` | PDF/Excel/CSV 다운로드 |
| 17 | 주석 정보 마트 | `/reports/disclosure-notes` | 주석 보고 매트릭스, 전표 드릴다운 |
| 18 | 규제 보고 제출 | `/reports/regulatory` | 금감원(FSS) 등 외부 기관 제출 |

### 3-5. 지출관리 (OPERATIONS) — `expenditure-resolution` + `payable` + `receivable`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 19 | 지출결의서 작성 | `/expenditure/resolution` | 결의서 작성, 항목별 금액 입력 |
| 20 | 지출 승인 워크플로 | `/expenditure/approval` | 기안→검토→승인→반려 |
| 21 | 매입채무(AP) 관리 | `/expenditure/payable` | AP 목록, 만기일, 연령 분석 |
| 22 | 매출채권(AR) 관리 | `/expenditure/receivable` | AR 목록, 연체 현황, 회수율 |
| 23 | 지급 실행 | `/expenditure/payment` | Payment Run 제안 → 은행 지급 |
| 24 | 선급금 관리 | `/expenditure/advance` | 선급금 등록, AP 상계 |
| 25 | 수금 관리 | `/expenditure/collection` | 수금 등록, 자동/수동 매칭 |
| 26 | 예산 편성/통제 | `/expenditure/budget` | 부서별 예산 vs 실행 모니터링 |
| 27 | 자금 수지 계획 | `/expenditure/cashflow` | 입출금 예정 스케줄, 유동성 예측 |

### 3-6. 세무관리 (OPERATIONS) — `tax`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 28 | 매입 세금계산서 | `/tax/purchase` | 매입 세금계산서 등록/수정/삭제 |
| 29 | 매출 세금계산서 | `/tax/sales` | 매출 세금계산서 발행/관리 |
| 30 | 부가세 신고 기초 | `/tax/vat` | 부가세 기초 데이터 집계, 신고서 미리보기 |
| 31 | 국세청 승인 조회 | `/tax/nts-verification` | NTS 승인번호 검증, 전자계산서 상태 |

### 3-7. 이연·대출·부대손익관리 (CREDIT) — `loan` + `deposit`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 32 | 대출 계약 관리 | `/loan/contracts` | 대출 계약 생성/조회, 조건 설정 |
| 33 | 대출 실행(지급) | `/loan/disbursal` | 대출금 지급, 회계 전표 자동 생성 |
| 34 | 이연 수수료/원가 | `/loan/deferred` | 이연 항목 유형, 등록, 초기 분개 |
| 35 | EIR 상각 스케줄 | `/loan/amortization` | 유효이자율 상각표 생성/조회 |
| 36 | 대출 이벤트 처리 | `/loan/events` | 조기상환, 금리 변경, EIR 재계산 |
| 37 | 예금 계좌 관리 | `/loan/deposit` | 예금 계좌 개설, 거래 내역, 이자 스케줄 |

### 3-8. 공정가치관리 (CREDIT) — `asset-lease`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 38 | 고정자산 대장 | `/fair-value/assets` | 자산 등록, 취득원가, 내용연수, 상각 방법 |
| 39 | 감가상각 실행 | `/fair-value/depreciation` | 월말 감가상각 배치, 상각 스케줄 조회 |
| 40 | 자산 처분 | `/fair-value/disposal` | 매각/폐기, 처분 손익 계산 |
| 41 | 자산 재평가/손상 | `/fair-value/revaluation` | 공정가치 조정, 손상차손 IAS 36 |
| 42 | IFRS 16 리스 계약 | `/fair-value/lease` | 리스 등록, ROU 자산/리스 부채 산출 |
| 43 | 리스 월결산 | `/fair-value/lease-monthly` | 이자비용, ROU 감가상각 실행 |
| 44 | 리스 재측정 | `/fair-value/lease-remeasure` | 조건 변경, 부채 재측정, 이력 |

### 3-9. 대손충당금관리 (RISK) — `ecl`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 45 | ECL 배치 관제탑 | `/ecl/batch` | IFRS 9 7단계 파이프라인 실행/모니터링 |
| 46 | 여신 익스포저 | `/ecl/exposures` | 여신 목록, 담보 배분, Stage 분류 |
| 47 | 모델 파라미터 설정 | `/ecl/parameters` | PD, LGD, CCF/EAD 파라미터 |
| 48 | ECL 산출 결과 | `/ecl/results` | ECL 요약 대시보드, 개별 시뮬레이션 |
| 49 | EAD 엔진 검증 | `/ecl/ead` | EAD 배치 실행, 검증 그리드 |

### 3-10. 마트데이터관리 및 대사 (RISK) — `account-mart` + `reconciliation`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 50 | 마트 탐색기 | `/mart/explorer` | 통합 마트 Grid, 포트폴리오 |
| 51 | DQ 감사 | `/mart/dq-audit` | DQ 규칙 위반, 품질 스코어카드 |
| 52 | 환율 관리 | `/mart/exchange-rates` | 일별 환율 등록/조회 |
| 53 | 시장금리 관리 | `/mart/market-rates` | SOFR, CD, 국고채 등 |
| 54 | 수익률곡선 관리 | `/mart/yield-curves` | Yield Curve, 보간법 설정 |
| 55 | 대사 규칙 설정 | `/mart/reconciliation-setup` | 대사 단위, 매칭 규칙, 허용 오차 |
| 56 | 대사 실행 | `/mart/reconciliation-run` | 대사 트리거, 결과 요약 |
| 57 | 차이 해소 워크벤치 | `/mart/reconciliation-diff` | 차이 목록, 담당자, 사유코드, 해소 |

### 3-11. 계정과목 코드 관리 (MASTER) — `master-data`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 58 | 계정과목 체계 | `/account-code/tree` | 5단계 계층 트리, 코드 검색, 속성 |
| 59 | 계정과목 등록/수정 | `/account-code/manage` | 신규 등록, 속성 편집, 활성/비활성 |
| 60 | 상품 코드 관리 | `/account-code/products` | 금융상품 코드 CRUD |

### 3-12. 거래처 관리 (MASTER) — `master-data`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 61 | 거래처 조회/등록 | `/partner/list` | 고객/공급자, 사업자번호, 등록 |
| 62 | 기준정보 변경 승인 | `/partner/approval` | Before/After Diff, 승인/반려 |

### 3-13. 내부회계 관리 (SYSTEM) — `governance`
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 63 | 감사 로그 탐색기 | `/governance/audit-logs` | 시스템 감사 이벤트, 필터 |
| 64 | 역할/권한 매트릭스 | `/governance/rbac` | 역할 정의, 권한 매핑 |
| 65 | 내부통제 체크리스트 | `/governance/controls` | 내부회계 점검, 통제 이력 |

### 3-14. 시스템관리 (SYSTEM) — admin
| # | 화면명 | 경로 | 설명 |
|---|---|---|---|
| 66 | 사용자 관리 | `/system/users` | 사용자 목록, 역할, 비밀번호 |
| 67 | 부서 관리 | `/system/departments` | 조직도, 코스트센터 |
| 68 | 메뉴 권한 관리 | `/system/menus` | 역할별 메뉴 접근 매트릭스 |
| 69 | 시스템 로그 | `/system/logs` | 접근/예외/배치 로그 |

---

## 4. 디렉터리 구조

```
src/
├── app/
│   ├── page.tsx                          # 대시보드 (/)
│   ├── layout.tsx                        # 루트 레이아웃
│   ├── globals.css
│   ├── ledger/                           # ← journal-ledger
│   ├── closing/                          # ← closing
│   ├── reports/                          # ← reporting
│   ├── expenditure/                      # ← expenditure + payable + receivable
│   ├── tax/                              # ← tax
│   ├── loan/                             # ← loan + deposit
│   ├── fair-value/                       # ← asset-lease
│   ├── ecl/                              # ← ecl
│   ├── mart/                             # ← account-mart + reconciliation
│   ├── account-code/                     # ← master-data (계정)
│   ├── partner/                          # ← master-data (거래처)
│   ├── governance/                       # ← governance
│   └── system/                           # ← admin
│
├── components/
│   ├── layout/
│   │   ├── MainLayout.tsx
│   │   ├── Sidebar.tsx
│   │   ├── TopHeader.tsx
│   │   ├── Footer.tsx
│   │   └── menus/                        # ← 백엔드 모듈별 메뉴 정의
│   │       ├── types.ts
│   │       ├── index.ts
│   │       ├── dashboard.ts
│   │       ├── ledger.ts                 # journal-ledger
│   │       ├── closing.ts               # closing
│   │       ├── reports.ts               # reporting
│   │       ├── expenditure.ts           # expenditure + payable + receivable
│   │       ├── tax.ts                   # tax
│   │       ├── loan.ts                  # loan + deposit
│   │       ├── fair-value.ts            # asset-lease
│   │       ├── ecl.ts                   # ecl
│   │       ├── mart.ts                  # account-mart + reconciliation
│   │       ├── account-code.ts          # master-data
│   │       ├── partner.ts               # master-data
│   │       ├── governance.ts            # governance
│   │       └── system.ts               # admin
│   ├── ui/                               # 공통 UI 컴포넌트
│   ├── charts/                           # 차트 컴포넌트
│   └── domain/                           # 도메인 특화 컴포넌트
│
├── mocks/                                # 백엔드 모듈별 Mock 데이터
├── services/                             # API 서비스 (Mock ↔ API 전환)
└── context/
    └── NavContext.tsx                    # 네비게이션 + RBAC
```

---

## 5. 작업 단위 및 커밋 계획

| 커밋 # | 단위 | 포함 내용 |
|---|---|---|
| 1 | 설계서 + 인프라 | 설계서 MD, NavContext, TopHeader, Sidebar, 메뉴 모듈 파일 |
| 2 | 공통 컴포넌트 | ui/, charts/, domain/ 컴포넌트 |
| 3 | Mock 데이터 | mocks/ 전체 모듈별 Mock |
| 4 | 원장관리 화면 | ledger/ 6개 페이지 |
| 5 | 결산관리 화면 | closing/ 7개 페이지 |
| 6 | 보고서관리 화면 | reports/ 4개 페이지 |
| 7 | 지출관리 화면 | expenditure/ 9개 페이지 |
| 8 | 세무관리 화면 | tax/ 4개 페이지 |
| 9 | 이연·대출 화면 | loan/ 6개 페이지 |
| 10 | 공정가치 화면 | fair-value/ 7개 페이지 |
| 11 | 대손충당금 화면 | ecl/ 5개 페이지 |
| 12 | 마트/대사 화면 | mart/ 8개 페이지 |
| 13 | 기준정보 화면 | account-code/ 3개 + partner/ 2개 |
| 14 | 내부회계+시스템 | governance/ 3개 + system/ 4개 |
| 15 | 정리 | 기존 라우트 정리, 문서 업데이트 |

---

## 6. 디자인 시스템

- **Dark Mode 기본**: 배경 `#020617`, 텍스트 `#f8fafc`
- **Glassmorphism**: `.glass-card` (bg-white/[0.03], backdrop-blur-xl, border-white/10, rounded-[32px])
- **컬러**: Primary Blue `#3b82f6`, 성공 `#22c55e`, 경고 `#f59e0b`, 위험 `#ef4444`
- **타이포**: Inter, 금액은 고정폭 모노스페이스
- **인터랙션**: hover `scale(1.02)`, click `scale(0.98)`, transition `300ms`
- **금액**: 천단위 콤마, 통화 코드 접두사, 음수 빨간색
