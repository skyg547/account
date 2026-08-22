# 🔗 Frontend ↔ Backend MSA 16대 모듈 API 정합성 명세서 (API Parity Matrix)

본 문서는 **Next.js 15 프론트엔드 서비스 레이어(rontend/src/services/*.ts)**와 **백엔드 16대 마이크로서비스(MSA)의 @RestController 엔드포인트** 간의 HTTP Method, URL Path, Request/Response DTO 구조 및 오프라인 Fallback Mock 정합성을 전수 대사한 명세서입니다.

---

## 1. 🏛️ API 통신 아키텍처 원칙 (Architecture Principles)

1. **API Gateway 단일 진입점:** 모든 프론트엔드 요청은 /api/v1/{module}/** 경로를 통해 API Gateway(포트 8000)로 라우팅됩니다.
2. **1.5초 AbortController 타임아웃 가드:** 백엔드가 기동되지 않은 로컬 단독 프론트엔드 개발 환경에서도 화면이 중단되지 않도록 1.5초 타임아웃 시 안전한 결정론적 Mock 데이터(src/mocks/)로 자동 Fallback합니다.
3. **CamelCase ↔ SnakeCase/DTO 정합성:** 프론트엔드 TypeScript 인터페이스와 백엔드 Java DTO 간 필드명 및 데이터 타입을 1:1로 일치시킵니다.

---

## 2. 📊 13대 프론트엔드 서비스 ↔ 백엔드 Controller 전수 대사표

| # | 프론트엔드 서비스 (src/services/) | 백엔드 모듈 | 담당 백엔드 Controller | 엔드포인트 URL | HTTP Method | 주요 기능 |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | uthService.ts | uth:api | AuthController | /api/v1/auth/login<br>/api/v1/auth/refresh<br>/api/v1/auth/logout<br>/api/v1/auth/tokens/pat | POST<br>POST<br>POST<br>GET/POST | 사용자 인증, JWT 토큰 갱신, 로그아웃, PAT 발급 |
| **2** | dminService.ts | uth:api<br>internal-audit:api | AuthAdminController<br>EvaluationController | /api/v1/admin/users<br>/api/v1/admin/roles<br>/api/v1/audit/logs | GET/POST<br>GET/PUT<br>GET | 시스템 사용자/권한 관리, 접근 감사 로그 조회 |
| **3** | journalService.ts | journal-ledger:api | JournalController | /api/v1/journals<br>/api/v1/journals/{id}<br>/api/v1/journals/{id}/post<br>/api/v1/journals/{id}/reverse | GET/POST<br>GET<br>POST<br>POST | 전표(Journal Entry) 입력, 원장 전기, 역분개(취소) |
| **4** | closingService.ts | closing:api | ClosingController | /api/v1/closing/periods<br>/api/v1/closing/run<br>/api/v1/closing/status<br>/api/v1/closing/locks | GET<br>POST<br>GET<br>GET/POST | 일마감/월마감 실행, 결산 게이트 검증, 마감 잠금 |
| **5** | ankingService.ts | deposit:api<br>loan:api | DepositController<br>LoanController | /api/v1/deposit/accounts<br>/api/v1/deposit/accounts/{id}/transact<br>/api/v1/loan/contracts<br>/api/v1/loan/contracts/{id}/accrue | GET/POST<br>POST<br>GET/POST<br>POST | 수신 계좌 개설/거래, 여신 대출 실행 및 이자 계산 |
| **6** | payableService.ts | payable:api<br>expenditure-resolution:api | PaymentController<br>ExpenditureResolutionController | /api/v1/payable/invoices<br>/api/v1/payable/payment-runs<br>/api/v1/expenditure/resolutions | GET/POST<br>POST<br>GET/POST | 매입채무 송장 등록, 지급 실행(Payment Run), 지출결의 |
| **7** | eceivableService.ts | eceivable:api | SalesController | /api/v1/receivable/invoices<br>/api/v1/receivable/receipts<br>/api/v1/receivable/aging | GET/POST<br>POST<br>GET | 매출채권 청구서 발행, 수납(Receipt), 연체 에이징 분석 |
| **8** | 	axService.ts | 	ax:api | TaxInvoiceController | /api/v1/tax/invoices<br>/api/v1/tax/invoices/validate<br>/api/v1/tax/summary | GET/POST<br>POST<br>GET | 세금계산서 발행, 금액 정합성(공급+세액=합계) 검증, 부가세 집계 |
| **9** | ssetService.ts | sset-lease:api | FixedAssetController | /api/v1/assets/fixed-assets<br>/api/v1/assets/fixed-assets/{id}/depreciate<br>/api/v1/assets/disposals | GET/POST<br>POST<br>POST | 유형고정자산 취득, 감가상각 실행, 자산 처분 |
| **10** | leaseService.ts | sset-lease:api | LeaseContractController | /api/v1/leases/contracts<br>/api/v1/leases/schedules<br>/api/v1/leases/{id}/modify | GET/POST<br>GET<br>PUT | IFRS16 리스 계약 등록, 사용권자산 상각 스케줄, 계약 변경 |
| **11** | eportingService.ts | eporting:api | FinancialReportController | /api/v1/reports/balance-sheet<br>/api/v1/reports/income-statement<br>/api/v1/reports/trial-balance | GET<br>GET<br>GET | 재무상태표(B/S), 포괄손익계산서(I/S), 합계잔액시산표 집계 |
| **12** | masterDataService.ts | master-data:api | AccountSubjectController<br>BusinessPartnerController | /api/v1/master/accounts<br>/api/v1/master/partners<br>/api/v1/master/departments | GET/POST<br>GET/POST<br>GET | 계정과목 체계(COA), 거래처 마스터, 부서 기준정보 |
| **13** | xService.ts | journal-ledger:api | FxRateController | /api/v1/fx/rates<br>/api/v1/fx/rates/latest<br>/api/v1/fx/convert | GET/POST<br>GET<br>POST | 통화별 고시환율 관리, 일자별 환율 조회, 외화 환산 계산 |

---

## 3. 🧪 프론트엔드 검증 명령

`powershell
# Next.js 15 빌드 검증 (122개 전체 정적/동적 라우트 컴파일)
cd frontend
npm run build
`
