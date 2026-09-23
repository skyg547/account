# Frontend ↔ Backend API 정합성 전수 감사 — Issue #179

기준: `origin/main@34c75839af2edf10f80ff60d8f24e89504d69e9e` · 감사일: 2026-09-10 · [Issue #179](https://github.com/skyg547/account/issues/179) · [이전 PR 반려 작업지시](https://github.com/skyg547/account/pull/548#issuecomment-5393624546).

**현재 서비스 API가 모두 일치하지 않는다.** 15개 TypeScript 파일에서 42개 endpoint 호출과 3개 transport helper 호출을 확인했다. Issue #740 반영 후 42개 중 36개는 HTTP method + Controller mapping이 존재하고, 6개는 `MISSING`이다. 경로가 존재하는 경우에도 아래 DTO/query/응답 처리 drift가 남는다. 이 문서는 정적 매핑 결과이며 모든 API의 Gateway 노출이나 DTO 호환을 주장하지 않는다.

## 범위와 판정 기준

- 기존 13개 domain service와 추가된 `apiClient.ts`, `browserSession.ts`를 모두 포함한다. 컴포넌트/페이지에서 직접 호출하는 API는 이슈의 `src/services/` 범위 밖이다.
- `MAPPED`는 Java Controller의 class + method 경로와 HTTP method의 정적 일치만 뜻한다. Gateway 경로, 실행 모듈 등록, 권한, 요청 유효성, DTO 적합성, 실제 서비스 가동 여부와는 별도로 판정한다.
- `MISSING`은 같은 method/path의 Controller가 없다는 뜻이다. 비슷한 이름의 다른 POST를 GET 대안으로 간주하지 않는다. `DRIFT`는 필드/enum/nullability/query/응답 처리 차이, `TODO`는 런타임 직렬화 등 아직 증명하지 않은 조건이다.
- 모든 JSON은 별도 envelope가 없는 객체/배열이다. TypeScript 선언과 `response.json()` 캐스팅은 런타임 스키마 검증이 아니다. `field?: T`는 누락/undefined를 허용하지만 JSON null을 허용하지 않는다.
- Java `Long` ↔ JS `number`는 안전 정수 범위가 다르다. `BigDecimal` ↔ `number`는 소수 정밀도 보장을 동일시할 수 없다. `LocalDate`/`LocalDateTime`/`Instant` ↔ `string`은 날짜 형식 제약이 추가된다. 아래 '명목 일치'는 이러한 제약을 포함한다. 금액 계산/반올림 로직은 변경하지 않는다.
- Java 참조형의 non-null 보장은 선언만으로 추정하지 않고 validation, mapper, 생성/조회 경로를 구분한다. 검증 annotation이 없는 응답 DTO의 null 가능성은 명시적으로 기록한다.
- backend-only 부록은 대응 Controller의 service 미사용 mapping 목록이다. 전체 백엔드 API가 프런트에서 사용되어야 한다는 요구는 아니다.

## 전수 endpoint inventory

URL의 `{...}`는 호출 시 값이 대입되는 자리다. query는 별도 열에도 명시한다. 아래 42개 행의 ID는 소스 call-site와 일대일 대응하며, 후반 재실행 검사로 누락/중복을 확인한다. DTO 상세는 각 서비스 이름 절을 참조한다.

| ID / 실제 호출 소스 | Method / 실제 URL template | Query | Controller 근거와 판정 |
|---|---|---|---|
| <!-- endpoint:adminService:39 --> [frontend/src/services/adminService.ts:39](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/adminService.ts#L39) | `GET /api/admin/users` | — | **MISSING** |
| <!-- endpoint:adminService:70 --> [frontend/src/services/adminService.ts:70](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/adminService.ts#L70) | `POST /api/audit/approvals/requests` | — | MAPPED · [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:134](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L134) |
| <!-- endpoint:assetService:49 --> [frontend/src/services/assetService.ts:49](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/assetService.ts#L49) | `GET /api/fixed-assets?status={status?}` | optional `status:string` | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java:76](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java#L76) |
| <!-- endpoint:assetService:66 --> [frontend/src/services/assetService.ts:66](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/assetService.ts#L66) | `GET /api/fixed-assets/{id}` | — | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java:81](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java#L81) |
| <!-- endpoint:assetService:75 --> [frontend/src/services/assetService.ts:75](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/assetService.ts#L75) | `POST /api/fixed-assets` | — | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java:33](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java#L33) |
| <!-- endpoint:assetService:88 --> [frontend/src/services/assetService.ts:88](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/assetService.ts#L88) | `POST /api/fixed-assets/depreciate/{processDate}` | — | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java:55](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java#L55) |
| <!-- endpoint:assetService:99 --> [frontend/src/services/assetService.ts:99](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/assetService.ts#L99) | `POST /api/fixed-assets/dispose` | — | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java:63](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/FixedAssetController.java#L63) |
| <!-- endpoint:authService:21 --> [frontend/src/services/authService.ts:21](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/authService.ts#L21) | `POST /api/auth/login` | — | MAPPED · [auth/api/src/main/java/com/ho/account/auth/api/web/AuthController.java:49](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/auth/api/src/main/java/com/ho/account/auth/api/web/AuthController.java#L49) |
| <!-- endpoint:bankingService:23 --> [frontend/src/services/bankingService.ts:23](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/bankingService.ts#L23) | `GET /api/finance/banking/inter-branch/dashboard` | — | **MISSING** |
| <!-- endpoint:bankingService:48 --> [frontend/src/services/bankingService.ts:48](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/bankingService.ts#L48) | `POST /api/finance/banking/inter-branch/auto-match` | — | **MISSING** |
| <!-- endpoint:closingService:35 --> [frontend/src/services/closingService.ts:35](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/closingService.ts#L35) | `GET /api/closing/calendars/{calendarId}/tasks` | — | MAPPED · [ClosingController.getClosingTasksByCalendarId](../../closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L90) |
| <!-- endpoint:closingService:54 --> [frontend/src/services/closingService.ts:54](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/closingService.ts#L54) | `PUT /api/closing/tasks/{taskId}/status` | — | MAPPED · [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:105](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L105) |
| <!-- endpoint:closingService:77 --> [frontend/src/services/closingService.ts:77](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/closingService.ts#L77) | `POST /api/closing/valuation-batches/run` | — | MAPPED · [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:196](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L196) |
| <!-- endpoint:closingService:95 --> [frontend/src/services/closingService.ts:95](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/closingService.ts#L95) | `POST /api/closing/provision-batches/run` | — | MAPPED · [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:209](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L209) |
| <!-- endpoint:fxService:32 --> [frontend/src/services/fxService.ts:32](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/fxService.ts#L32) | `GET /api/fx/dashboard` | — | **MISSING** |
| <!-- endpoint:journalService:30 --> [frontend/src/services/journalService.ts:30](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/journalService.ts#L30) | `GET /api/journals` | — | MAPPED · [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java:65](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java#L65) |
| <!-- endpoint:journalService:65 --> [frontend/src/services/journalService.ts:65](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/journalService.ts#L65) | `POST /api/journals` | — | MAPPED · [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java:32](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java#L32) |
| <!-- endpoint:leaseService:53 --> [frontend/src/services/leaseService.ts:53](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/leaseService.ts#L53) | `GET /api/ifrs16/leases` | — | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java:54](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java#L54) |
| <!-- endpoint:leaseService:106 --> [frontend/src/services/leaseService.ts:106](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/leaseService.ts#L106) | `GET /api/ifrs16/leases/{id}` | — | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java:59](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java#L59) |
| <!-- endpoint:leaseService:115 --> [frontend/src/services/leaseService.ts:115](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/leaseService.ts#L115) | `POST /api/ifrs16/leases` | — | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java:26](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java#L26) |
| <!-- endpoint:leaseService:128 --> [frontend/src/services/leaseService.ts:128](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/leaseService.ts#L128) | `POST /api/ifrs16/leases/process-monthly/{processDate}` | — | MAPPED · [asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java:66](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java#L66) |
| <!-- endpoint:masterDataService:119 --> [frontend/src/services/masterDataService.ts:119](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/masterDataService.ts#L119) | `GET /api/basic/account-subjects` | — | MAPPED · [master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java:32](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java#L32) |
| <!-- endpoint:masterDataService:127 --> [frontend/src/services/masterDataService.ts:127](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/masterDataService.ts#L127) | `GET /api/basic/businesspartners` | — | MAPPED · [master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:44](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L44) |
| <!-- endpoint:masterDataService:135 --> [frontend/src/services/masterDataService.ts:135](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/masterDataService.ts#L135) | `GET /api/master-data/change-requests/pending` | — | MAPPED · [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:61](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L61) |
| <!-- endpoint:masterDataService:148 --> [frontend/src/services/masterDataService.ts:148](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/masterDataService.ts#L148) | `POST /api/master-data/change-requests` | — | MAPPED · [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:51](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L51) |
| <!-- endpoint:masterDataService:164 --> [frontend/src/services/masterDataService.ts:164](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/masterDataService.ts#L164) | `POST /api/master-data/change-requests/{id}/approve` | — | MAPPED · [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:70](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L70) |
| <!-- endpoint:masterDataService:181 --> [frontend/src/services/masterDataService.ts:181](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/masterDataService.ts#L181) | `POST /api/master-data/change-requests/{id}/reject` | — | MAPPED · [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:79](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L79) |
| <!-- endpoint:payableService:26 --> [frontend/src/services/payableService.ts:26](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/payableService.ts#L26) | `GET /api/payable/invoices?status={status?}` | optional `status:string` | MAPPED · [PurchaseController.getPurchaseInvoices](../../payable/api/src/main/java/com/ho/account/expenditure/payable/api/adapter/in/web/PurchaseController.java#L50) |
| <!-- endpoint:payableService:45 --> [frontend/src/services/payableService.ts:45](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/payableService.ts#L45) | `GET /api/payable/invoices/{id}` | — | MAPPED · [PurchaseController.getPurchaseInvoiceById](../../payable/api/src/main/java/com/ho/account/expenditure/payable/api/adapter/in/web/PurchaseController.java#L62) |
| <!-- endpoint:receivableService:25 --> [frontend/src/services/receivableService.ts:25](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/receivableService.ts#L25) | `GET /api/receivable/invoices?status={status?}` | optional `status:string` | **MISSING** |
| <!-- endpoint:receivableService:44 --> [frontend/src/services/receivableService.ts:44](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/receivableService.ts#L44) | `GET /api/receivable/invoices/{id}` | — | **MISSING** |
| <!-- endpoint:reportingService:59 --> [frontend/src/services/reportingService.ts:59](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/reportingService.ts#L59) | `POST /api/v1/reporting/generate?type={type}&baseDate={baseDate}` | required `type`, `baseDate` | MAPPED · [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:46](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L46) |
| <!-- endpoint:reportingService:72 --> [frontend/src/services/reportingService.ts:72](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/reportingService.ts#L72) | `POST /api/v1/reporting/generate/document?type={type}&baseDate={baseDate}&format={format}` | required `type`, `baseDate`; `format` PDF/EXCEL | MAPPED · [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:58](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L58) |
| <!-- endpoint:reportingService:90 --> [frontend/src/services/reportingService.ts:90](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/reportingService.ts#L90) | `GET /api/v1/reporting/disclosure-notes?type={type}&baseDate={baseDate}` | required `type`, `baseDate` | MAPPED · [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:98](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L98) |
| <!-- endpoint:reportingService:101 --> [frontend/src/services/reportingService.ts:101](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/reportingService.ts#L101) | `POST /api/v1/reporting/disclosure-notes/generate?type={type}&baseDate={baseDate}` | required `type`, `baseDate` | MAPPED · [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:88](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L88) |
| <!-- endpoint:reportingService:113 --> [frontend/src/services/reportingService.ts:113](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/reportingService.ts#L113) | `GET /api/v1/reporting/disclosure-notes/drill-down?type={type}&baseDate={baseDate}&entryId={entryId}` | required `type`, `baseDate`; `entryId` | MAPPED · [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:109](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L109) |
| <!-- endpoint:taxService:37 --> [frontend/src/services/taxService.ts:37](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/taxService.ts#L37) | `GET /api/ap/invoices?startDate={startDate}&endDate={endDate}` | required `startDate`, `endDate` ISO dates | MAPPED · [tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java:63](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java#L63) |
| <!-- endpoint:taxService:54 --> [frontend/src/services/taxService.ts:54](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/taxService.ts#L54) | `GET /api/ap/invoices/{id}` | — | MAPPED · [tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java:47](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java#L47) |
| <!-- endpoint:taxService:63 --> [frontend/src/services/taxService.ts:63](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/taxService.ts#L63) | `GET /api/ap/invoices/issue-id/{issueId}` | — | MAPPED · [tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java:55](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java#L55) |
| <!-- endpoint:taxService:72 --> [frontend/src/services/taxService.ts:72](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/taxService.ts#L72) | `POST /api/ap/invoices` | — | MAPPED · [tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java:42](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java#L42) |
| <!-- endpoint:taxService:85 --> [frontend/src/services/taxService.ts:85](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/taxService.ts#L85) | `PUT /api/ap/invoices/{id}` | — | MAPPED · [tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java:72](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java#L72) |
| <!-- endpoint:taxService:98 --> [frontend/src/services/taxService.ts:98](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/frontend/src/services/taxService.ts#L98) | `DELETE /api/ap/invoices/{id}` | — | MAPPED · [tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java:78](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java#L78) |

### 파일별 건수와 helper 경계

| 파일 | Endpoint 호출 | Helper 호출 |
|---|---:|---:|
| `adminService.ts` | 2 | 0 |
| `apiClient.ts` | 0 | 2 |
| `assetService.ts` | 5 | 0 |
| `authService.ts` | 1 | 0 |
| `bankingService.ts` | 2 | 0 |
| `browserSession.ts` | 0 | 0 |
| `closingService.ts` | 4 | 0 |
| `fxService.ts` | 1 | 0 |
| `journalService.ts` | 2 | 0 |
| `leaseService.ts` | 4 | 0 |
| `masterDataService.ts` | 6 | 1 |
| `payableService.ts` | 2 | 0 |
| `receivableService.ts` | 2 | 0 |
| `reportingService.ts` | 5 | 0 |
| `taxService.ts` | 6 | 0 |

`apiClient.ts:50`의 `fetch(url, fetchInit)`과 `:93`의 `fetchWithTimeout(url, options)`은 전달 helper이고 독립 endpoint가 아니다. `masterDataService.ts:66`의 `fetch(apiUrl(url), init)`는 6개 requestJson 호출을 실행하는 helper다. `browserSession.ts`는 localStorage의 과거 키를 지우는 함수만 가지며 네트워크 호출은 없다. `adminService.requestUserOnboarding:103`도 로컬 결과만 생성한다.

`apiClient.fetchWithTimeout`의 기본 timeout은 **15,000ms**이다. Closing batch 호출은 20,000ms를 지정한다. Timeout/network 오류를 던지고 토스트를 낼 수 있지만 mock을 생성하지 않는다. HTTP 비정상 상태도 Response 그대로 반환한다. `requestJsonWithTimeout`은 비정상 HTTP에서 ApiError를 던지지만 현재 services의 endpoint 호출에 사용되지 않는다. 실제 mock/fallback은 아래 개별 service에서만 발생한다.

BFF는 same-origin `/api`를 받아 인증된 요청을 전달한다. 브라우저의 `X-User-ID`/Authorization 헤더는 신원 근거로 전달하지 않는다. `authService` 응답은 BFF가 Auth 응답에서 token/tokenType을 제거한 계약이다. logout은 BFF 로컬 route이며 services 내 auth login과 혼동하지 않는다. `gateway/src/main/java/com/ho/account/gateway/filter/JwtAuthenticationFilter.java:123`은 검증된 principal.username으로 `X-Auth-User`와 `X-User-ID`를 새로 주입한다. 선언된 Controller와 실제 Gateway/실행 모듈 노출 여부는 별개이며 live smoke는 이번 감사에서 실행하지 않았다.

## authService / adminService / masterDataService DTO 대조

이 절의 경로 약어: `FS=frontend/src/services`, `AD=auth/api/src/main/java/com/ho/account/auth/api/dto`, `MD=master-data/api/src/main/java/com/ho/account/masterdata/api/dto`, `MC=master-data/core/src/main/java/com/ho/account/masterdata/core`, `SW=shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web`. 경로 뒤 `:숫자`는 기준 커밋의 소스 라인이다.

### authService

근거: `FS/authService.ts:1`, `AD/LoginRequest.java:5`, `AD/LoginResponse.java:6`, `frontend/src/server/auth/bff.ts:382,415,459,487`.

| 필드 | FE / BFF | Backend / 판정 |
|---|---|---|
| request.username | required string; trim/nonblank | String @NotBlank; BFF max100 추가 검사 |
| request.password | required string; 공백 검사 후 원문 전달 | String (DTO @NotBlank 없음); BFF nonblank/max1024 검사 |
| request.loginType | service 없음; BFF NORMAL 추가 | String; 의도한 변환으로 일치 |
| request.otpCode | 없음 | nullable String; NORMAL 미사용 |
| response.expiresIn | number | long; BFF safe integer ≥1 검사 |
| response.username | string | String; BFF nonblank 검사 |
| response.departmentCode | string | String; BFF null/비문자열이면 502 |
| response.roles | string[] | List<String>; BFF 배열과 각 문자열 검사, enum 없음 |
| response.roleVersion | number | long; BFF safe integer ≥1 검사 |
| response.token, tokenType | 없음 | String; BFF 검증 후 HttpOnly cookie에 token 저장, 공개 JSON에서 두 필드 제거 |

공개 응답 5필드가 일치한다. blank 입력/비정상 HTTP/JSON 오류는 throw하며 mock은 없다. logout/refresh를 AuthController의 서비스 호출 목록에 추가하지 않는다.

### adminService

`FS/adminService.ts:7`의 `UserInfo`는 `id:number`, `name:string`, `email:string`, `role:UserRole`, `status:ACTIVE/PENDING/INACTIVE`, `lastLogin:string`, `dept:string`, `pendingRequestId?:string`이다. `UserRole`은 `frontend/src/context/NavContext.tsx:21`의 SYSTEM_ADMIN/ACCOUNTING_ADMIN/RISK_MANAGER/RISK_ANALYST/MASTER_MANAGER/AUDITOR/USER다. GET `/api/admin/users` Controller가 없어 **8개 필드 모두 대응 계약 부재**다.

승인 요청 근거: `FS/adminService.ts:59`, `SW/dto/MasterApprovalRequest.java:8`, `SW/dto/MasterApprovalResponse.java:7`, `shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/infrastructure/auth/AuthUserRoleApprovalApplyAdapter.java:35,65,107`.

| Request 필드 | FE 실제 값/타입 | BE |
|---|---|---|
| masterType | AUTH_USER_ROLE string | @NotBlank String, 지원 apply adapter 존재 |
| masterKey | user.email string | @NotBlank String |
| requestType | UPDATE | @NotNull CREATE/UPDATE/DELETE enum; UPDATE 수용 |
| payload | JSON.stringify 결과 string | nullable String; apply 시 typed JSON parse |
| requestUser | 고정 string | @NotBlank String; Controller가 본문 값을 command로 전달 |
| effectiveDate | UTC 날짜 slice string | nullable LocalDate; ISO date |
| requestedVersion | number 1 | nullable Integer |

nested payload의 `username:string`, `userId:string`, `role:UserRole`은 BE `AuthUserRolePayload`의 String 필드와 맞는다. BE-only nullable `roleCode:String`, `roles:List<String>`, `dataScope:String`, `validTo:Instant`는 생략한다. username→userId→masterKey 순으로 식별하고 roles가 있으면 role/roleCode보다 우선한다. role은 trim/uppercase/ROLE_ prefix로 정규화된다. user.email과 실제 Auth username의 의미적 동일성은 사용자 목록 API 부재로 TODO다.

| BE 응답 필드(전체) | Java 타입 | FE 변환 |
|---|---|---|
| id | Long 참조 | String(id ?? approvalId ?? generated) → requestId |
| status | PENDING/APPROVED/REJECTED | 같은 FE enum; nullish면 PENDING |
| effectiveDate | nullable LocalDate | nullish면 요청 날짜 |
| masterType, masterKey, payload, requestUser | String 참조 | 버림 |
| requestType | CREATE/UPDATE/DELETE enum | 버림 |
| requestDate | LocalDateTime 참조 | 버림 |
| approverUser, approvalDate | nullable String / LocalDateTime | 버림 |
| requestedVersion | nullable Integer | 버림 |
| remarks, auditUser | String 참조 | 버림 |

응답 record의 참조형에는 자체 nonnull 보장이 없다. `approvalId`는 BE 필드가 아니라 FE legacy fallback이다. 결과 `ApprovalRequestResult`의 requestId:string/status enum/effectiveDate:string에는 runtime 타입 검사가 없다.

`getUsers`는 HTTP/transport 오류 시 300ms 뒤 mock을 반환하지만 `return response.json()`을 await하지 않아 비동기 JSON parse 오류는 전파한다. `requestRoleChange`는 HTTP/transport/JSON 오류를 모두 local ID 발급 및 mock user PENDING 변경으로 대체한다(500ms). `requestUserOnboarding`은 API 없이 local PENDING 결과를 만든다.

AuditController mapping은 존재하지만 `gateway/src/main/resources/application.yml`의 선언 Path에 `/api/admin/**`, `/api/audit/**`가 없다. **기본 BFF→Gateway 경로 부재**이며 Controller 존재와 가동/노출을 동일시할 수 없다. BFF `bff.ts:17,335,504`는 브라우저 신원 헤더를 제거하고 cookie 기반 Authorization을 만든다. 고정 `X-User-ID`는 신원 검증 근거가 아니다.

### masterDataService: 계정과목

근거: `FS/masterDataService.ts:1`, `MD/AccountSubjectDto.java:8,46`, `MC/domain/model/AccountSubject.java:49`. GET 응답은 bare array, 중첩 없음.

| 필드 | FE | BE | 판정 |
|---|---|---|---|
| code, name | required string 각각 | String 각각 | 명목 일치; DTO 자체 null 방어 없음 |
| type | ASSET/LIABILITY/EQUITY/INCOME/EXPENSE | 없음 | DRIFT |
| category | GROUP/SUBJECT | ASSETS/LIABILITIES/EQUITY/REVENUE/EXPENSES | DRIFT |
| parentCode | optional string (null 불허) | 부모 없으면 명시적 null String | DRIFT |
| status | ACTIVE/INACTIVE | 없음 | DRIFT |
| validFrom, validTo | required string 각각 | LocalDate 각각 | 날짜 형식; DTO null 방어 없음 |
| balanceType | 없음 | DEBIT/CREDIT enum | BE-only |
| reportLine | 없음 | nullable String | BE-only |
| unsettled, fixedAsset | 없음 | primitive boolean 각각 | BE-only |

성공 JSON은 cast만 하므로 DTO drift가 mock fallback을 발생시키지 않는다.

### masterDataService: 거래처 response와 nested command

근거: `FS/masterDataService.ts:12`, `MD/BusinessPartnerDto.java:12`, `MC/application/command/BusinessPartnerCommand.java:13`, `MC/domain/model/BusinessPartner.java:68,94,512`.

| 필드 | FE command / response | Backend command / response |
|---|---|---|
| id | response만 required number | response만 Long |
| businessPartnerCode, businessPartnerName | required string 각각 | String 각각; domain nonblank 필수 |
| registrationNumber | required string | nullable String, blank→null; response @Masked(REG_NO): DRIFT |
| ceoName, businessType, businessItem | required string 각각 | nullable String 각각, blank→null: DRIFT |
| partnerType | CUSTOMER/VENDOR/BANK/OTHER_BP | 같은 enum; command null→OTHER_BP |
| useYn | boolean | Boolean; command null→true, domain response nonnull |
| kycStatus | PENDING/APPROVED/REJECTED/REVIEW_REQUIRED | 같은 enum; command null→PENDING |
| riskRating | LOW/MEDIUM/HIGH/CRITICAL | 같은 enum; command null→LOW |
| validFrom | string | LocalDate; command null→오늘 |
| validTo | string | LocalDate; command null→open-ended |

12개 command 필드가 FE에서는 모두 필수인 것은 유효한 요청 subset이다. 응답의 nullable 문자열 4개는 FE와 불일치다. 생성 요청의 payloadJson은 객체가 아닌 위 12개 필드를 담은 **JSON string**이다. `MC/application/service/BusinessPartnerMasterDataChangeApplier.java`의 적용 경로는 targetKey와 businessPartnerCode를 비교하고 바깥 effectiveDate를 validFrom으로 사용한다. FE는 두 날짜를 동일하게 보낸다. 응답 DTO에 domain accounts/audit/timestamps를 추정 추가하지 않는다.

### masterDataService: 변경 요청

근거: `FS/masterDataService.ts:35,148,164,181`, `MD/MasterDataChangeRequestDto.java:18`, `MD/MasterDataChangeRequestCreateDto.java:20`, `MD/MasterDataChangeDecisionDto.java:12`, `MC/domain/changerequest/MasterDataChangeRequest.java:59,101,120,222`.

| Response 필드(15개) | FE | BE / 판정 |
|---|---|---|
| id | number | Long; 저장 ID, safe-integer 미검사 |
| targetType | BUSINESS_PARTNER 또는 string | ACCOUNT_SUBJECT/BUSINESS_PARTNER/DEPARTMENT/PRODUCT/CURRENCY/EXCHANGE_RATE/FISCAL_PERIOD; FE가 더 넓음 |
| targetKey | string | nonblank String max100 |
| changeType | CREATE/UPDATE/DEACTIVATE | 같은 enum |
| status | REQUESTED/APPROVED/REJECTED/APPLIED | 같은 enum |
| effectiveDate | string | 필수 LocalDate |
| requestedVersion | number | 필수 positive Integer |
| requestedBy | string | 필수 String max80 |
| approvedBy | string 또는 null | nullable String |
| requestedAt | string | 생성 LocalDateTime; timezone offset 계약 아님 |
| approvedAt | string 또는 null | nullable LocalDateTime |
| reason | string 또는 null | nullable String max500; reject 필수 |
| payloadJson | string 또는 null | CREATE/UPDATE 필수 String, DEACTIVATE nullable |
| sourceReference | string 또는 null | nullable String max120 |
| appliedAt | string 또는 null | APPLIED 전 nullable LocalDateTime |

| Create 입력(7개) | FE | BE 제약 |
|---|---|---|
| targetType | BUSINESS_PARTNER | @NotNull MasterDataType |
| targetKey | partner.businessPartnerCode | @NotBlank String max100 |
| changeType | CREATE | @NotNull ChangeType |
| effectiveDate | partner.validFrom | @NotNull LocalDate |
| requestedVersion | 1 | @NotNull @Positive Integer; CREATE version1 |
| reason | trimmed string 또는 null | nullable String max500 |
| payloadJson | JSON.stringify(partner) | String; CREATE typed payload 검증 |

approve는 Long id 외 body 없음으로 일치. reject는 `{reason:string}`이며 FE trim/nonblank 검사, BE DTO max500 및 domain nonblank 검사다. requester는 Controller의 신뢰된 `X-Auth-User`에서 구성하므로 FE body에 없는 것이 누락은 아니다. create/approve/reject는 동일 response DTO를 사용한다. 승인과 적용은 별도 상태다.

조회 실패 시 getAccountSubjects/getBusinessPartners는 모든 요청/JSON 오류를 mock으로 대체하고, pending 조회는 오류를 `[]`로 대체한다. pending 성공 결과도 BUSINESS_PARTNER만 필터한다. 생성/승인/반려는 오류를 전파하고 mock이 없다. 프런트 README의 '목록 API 오류를 mock으로 숨기지 않는다' 설명과 현재 소스 동작은 다르며, 이 PR은 README의 기존 계약 설명을 임의 변경하지 않고 감사 링크만 추가한다.
## journalService / closingService / reportingService DTO 대조

경로 약어: `JD=journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalApiDto.java`, `CD=closing/api/src/main/java/com/ho/account/closing/dto`, `RD=reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/dto`, `RM=reporting/core/src/main/java/com/ho/account/reporting/domain/model`. FE 근거는 `frontend/src/services/<서비스>.ts`다.

### journalService

GET 호출 `:30`은 required query `startDate`, `endDate`를 보내지 않는다. `JournalController.java:65`는 둘 다 ISO LocalDate로 요구하므로 mapping이 있어도 요청이 유효하지 않다. POST `:65`는 DTO 변환 없이 JournalEntryDto를 그대로 전송한다.

| 계약 / 필드 | FE | BE / 차이 |
|---|---|---|
| Create root: slipDate | required string | @NotNull LocalDate (`JD:22`) |
| accountingDate | required string | nullable/unannotated LocalDate |
| description | required string | @NotBlank String |
| entryType | required string | optional String |
| currencyCode | optional string | optional String |
| details | required JournalDetailDto[] | **@NotEmpty lines:List<@Valid LineRequest>**: DRIFT |
| id, slipNo, status, createdBy | optional number/string/string/string | CreateRequest 없음; actor는 required header에서 취득 |
| exchangeRate | 없음 | optional @Positive BigDecimal, 11 integer/8 fraction digits |
| lineageSourceType, lineageSourceId | 없음 | optional String 각각 |
| Create line: side | DEBIT/CREDIT | @NotNull JournalSide; enum 일치 (`JD:49`) |
| accountCode | required string | @NotBlank String |
| amount | required number | required positive BigDecimal, 17 integer/2 fraction digits |
| baseAmount | **없음** | required positive BigDecimal, 같은 digits 제약: DRIFT |
| departmentCode, businessPartnerCode | optional string 각각 | optional String 각각 |
| detailDescription | optional string | **description:String**: DRIFT |
| Response root: id, slipNo | optional number/string | Long/String (`JD:81`) |
| slipDate, accountingDate | required string 각각 | LocalDate 각각 |
| description, entryType | required string 각각 | String 각각 |
| currencyCode, createdBy | optional string 각각 | String 각각 |
| status | optional string | enum name String 또는 명시적 null; DRAFT/REQUESTED/APPROVED/REJECTED/POSTED/REVERSED |
| details | required JournalDetailDto[] | **lines:LineView[]**: DRIFT |
| exchangeRate, auditUser, lineageSourceType, lineageSourceId | 없음 | BigDecimal/String/String/String |
| Response line: side | required DEBIT/CREDIT | JournalSide에서 name 또는 null (`JD:116,129`) |
| accountCode, amount | required string/number | String/BigDecimal |
| departmentCode, businessPartnerCode | optional string 각각 | nullable String 각각 |
| detailDescription | optional string | **description:String** |
| id, baseAmount | 없음 | Long/BigDecimal |

전표 응답의 Java 참조형은 자체 nonnull 제약이 없고 FE의 `?`는 null을 모델링하지 않는다. root/nested의 이름·필수 baseAmount 차이는 단순 precision 주의와 구분되는 실제 drift다. GET은 HTTP/transport 오류를 1개 mock으로 대체한다. `return response.json()`에 await가 없어 비동기 parse 오류는 fallback되지 않는다. POST는 오류 body를 읽어 throw하고 네트워크 오류도 rethrow한다. `Promise<JournalEntryDto | null>` 선언과 달리 실패를 null로 반환하지 않는다.

### closingService

GET `:35`의 `/api/closing/calendars/{calendarId}/tasks`는 Issue #740에서 `ClosingController.getClosingTasksByCalendarId`에 매핑되었다. GET과 기존 PUT은 모두 `ClosingTaskDto`를 사용하므로 아래 task DTO 차이가 두 응답에 동일하게 적용된다.

| 계약 / 필드 | FE | BE / 차이 |
|---|---|---|
| Update request status | PENDING/IN_PROGRESS/COMPLETED/FAILED/SKIPPED | @NotNull ClosingTaskStatus, enum 일치 (`CD/ClosingTaskStatusUpdateDto.java:12`) |
| Update request user | required string | @NotBlank String; Controller는 body user 사용 |
| Task response id, name | required number/string | Long/String (`CD/ClosingTaskDto.java:14`) |
| category | PRE_CLOSING/CLOSING_ENTRY/POST_CLOSING/REPORTING | 같은 enum, domain nullable |
| status | 위 task status enum | 같은 enum |
| assignedTo, dueDate | required string 각각 | nullable String/LocalDateTime (`closing/core/src/main/java/com/ho/account/closing/domain/ClosingTask.java:32`) |
| isMandatory | required boolean | MockMvc 직렬화 결과는 **mandatory**이므로 필드명이 다른 DRIFT (`ClosingControllerTest`) |
| errorMessage | optional string | BE 없음 |
| calendarId | 없음 | Long, mapper 명시적 null 허용 |
| description, completionConditionJson | 없음 | String 각각 |
| taskOrder | 없음 | Integer |
| createdAt, updatedAt, auditUser | 없음 | LocalDateTime/LocalDateTime/String |
| Valuation request fiscalPeriodId | required number | @NotNull Long (`CD/ValuationBatchRequestDto.java:13`) |
| valuationType | FX_RATE/FINANCIAL_INSTRUMENT | 같은 값 + INVENTORY |
| runBy | required string | @NotBlank @Size(max=50) String |
| Provision request fiscalPeriodId | required number | @NotNull Long (`CD/ProvisionBatchRequestDto.java:13`) |
| provisionType | ECL/BAD_DEBT/IMPAIRMENT | 같은 값 + WARRANTY/RESTRUCTURING |
| runBy | required string | @NotBlank @Size(max=50) String |

Task category/assignedTo/dueDate의 nullable은 필수 FE와 다르다. `isMandatory`는 새 GET의 MockMvc 직렬화 테스트에서 `mandatory`로 관측되어 FE 필드명과 다르다. task 상태 enum 값 일치와 유효 상태 전이 여부는 별개다(`ClosingTask.changeStatus:229`).

Batch 응답 전체는 FE가 버린다. boolean은 `response.ok`에서 만든 것이며 완료 상태 DTO가 아니다. 두 API는 201 `ValuationBatchDto`/`ProvisionBatchDto`를 반환한다(`CD/*BatchDto.java:14`). 전체 필드:

| BE 응답 필드 | Java 타입 / enum | FE |
|---|---|---|
| id, fiscalPeriodId, generatedJournalEntryId | Long 각각 | 모두 미소비 |
| fiscalYear, fiscalPeriod, reportLink, runBy, auditUser | String 각각 | 모두 미소비 |
| runDateTime, createdAt, updatedAt | LocalDateTime 각각 | 모두 미소비 |
| valuationType 또는 provisionType | 위 해당 enum | 미소비 |
| status | RUNNING/COMPLETED/FAILED/PENDING_APPROVAL | 미소비 |

참조 필드의 응답 nonnull annotation은 없다. Batch enum 근거는 `closing/core/src/main/java/com/ho/account/closing/domain/ValuationBatch.java:61`, `ProvisionBatch.java:61`이다. task GET은 HTTP/transport 오류에 7개 mock, PUT의 HTTP/transport 실패는 null, batch 실패는 false다. PUT 역시 성공 응답의 JSON parse를 await하지 않으므로 비동기 parse 오류는 null로 바뀌지 않고 전파된다. task GET 성공 JSON 비동기 parse 오류는 await되지 않아 mock으로 가지 않는다. task 조회 silentToast, batch timeout20초 설정이 실제 존재한다.

### reportingService

5개 요청은 JSON body 없이 `type`, `baseDate` query를 보낸다. BE는 ISO **DATE_TIME LocalDateTime**을 요구하므로 date-only string은 충분하지 않다. FE type은 BALANCE_SHEET/INCOME_STATEMENT, BE는 CASH_FLOW_STATEMENT도 지원한다(`RM/FinancialStatement.java:71`). 이는 FE의 유효 subset이며 지원되지 않는 FE 값을 보내는 오류와 구별한다. document는 PDF/EXCEL format 추가, drill-down은 entryId:string 추가다. 생성·문서·mart 생성은 BE의 신뢰된 X-User-ID가 필요하다.

| 응답 / 필드 | FE | BE / 차이 |
|---|---|---|
| FinancialStatement: statementId | string | String (`RD/FinancialStatementResponseDto.java:13`) |
| type | StatementType | enum, BE CASH_FLOW_STATEMENT 추가 |
| baseDate | string | LocalDateTime |
| lines | ReportLineDto[] | List<ReportLineResponseDto> |
| status | DRAFT/FINAL | 같은 enum |
| ReportLine: lineCode, label, noteNumber | string 각각 | String 각각, domain null 허용 (`RD/ReportLineResponseDto.java:12`, `RM/ReportLine.java:46`) |
| currentAmount, previousAmount | number 각각 | BigDecimal 각각; domain constructor null→zero, no-arg constructor도 존재 |
| level | number | primitive int |
| Mart root: martId, statementId, generatedBy | string 각각 | String 각각 (`RD/DisclosureNoteMartResponseDto.java:14`) |
| statementType | StatementType | enum subset 차이 |
| baseDate, generatedAt | string 각각 | LocalDateTime 각각 |
| entries | DisclosureNoteMartEntryDto[] | List<DisclosureNoteMartEntryResponseDto> |
| Mart entry: entryId, noteNumber, sourceLineCode, sourceLineLabel, currencyCode | string 각각 | String 각각 (`RD/DisclosureNoteMartEntryResponseDto.java:8`) |
| currentAmount, previousAmount | number 각각 | BigDecimal 각각 |
| noteCategory, maturityBucket, rateType, riskCategory | string 각각 | 아래 enum 각각; FE가 더 넓음 |
| martId, statementId, generatedBy | 없음 | String 각각 |
| statementType, baseDate, generatedAt | 없음 | enum/LocalDateTime/LocalDateTime |
| Drilldown: id, side | number/string | Long/JournalSide(DEBIT,CREDIT) (`RD/JournalDetailSummaryResponseDto.java:15`) |
| accountCode, accountName, slipNo, detailDescription, headerDescription | string 각각 | String 각각 |
| accountingDate, amount | string/number | LocalDate/BigDecimal |
| accountCategory, baseAmount, businessPartnerCode, accountNo | 없음 | String/BigDecimal/String/String |

Mart enum 전수(`RM/DisclosureNoteMartEntry.java:261`):

- NoteCategory: MATURITY, INTEREST_RATE, CURRENCY, RISK, GENERAL.
- MaturityBucket: ON_DEMAND, LESS_THAN_3_MONTHS, THREE_TO_TWELVE_MONTHS, ONE_TO_FIVE_YEARS, OVER_FIVE_YEARS, UNSPECIFIED, NOT_APPLICABLE.
- RateType: FIXED, FLOATING, NON_INTEREST_BEARING, UNSPECIFIED, NOT_APPLICABLE.
- RiskCategory: CREDIT, LIQUIDITY, MARKET, OPERATIONAL, NOT_APPLICABLE.

FinancialStatement root String/type/date에 domain nonnull 검증이 없고 ReportLine 문자열 null을 FE가 허용하지 않는다. 반면 `RM/DisclosureNoteMart.java:19` 및 `DisclosureNoteMartEntry.java:46`은 IDs/actor/enum/date/text를 검증하고 amount null을 정규화하므로 domain 생성 mart의 필수값 근거가 있다. Drilldown DTO 및 `contracts`의 JournalDetailSummary 참조 필드에는 nonnull 보장이 없고 FE는 null 대안을 선언하지 않는다. 중첩은 statement.lines와 mart.entries뿐이며 drilldown은 flat array다.

문서 응답은 bytes→Blob으로 수신한다. `reporting/core`의 `StatementDocumentRendererAdapter.java:28`은 PDF를 application/pdf/pdf로, EXCEL을 text/csv/csv로 렌더링한다. FE `reportingService.ts:83`의 두 format 분기는 모두 `.csv` 파일명이다. **PDF도 .csv로 저장하는 DRIFT**이며 'PDF가 CSV fallback'이라는 설명은 틀리다. 5개 함수 모두 HTTP 오류를 throw하고 mock/timeout wrapper가 없다. mart GET의 404도 throw한다.
## payableService / receivableService / taxService DTO 대조

경로 약어: `PD=payable/api/src/main/java/com/ho/account/expenditure/payable/api/dto`, `SD=receivable/api/src/main/java/com/ho/account/receivable/api/dto`, `TD=tax/api/src/main/java/com/ho/account/tax/api/dto`.

### payableService / receivableService

Payable의 GET 목록/단건은 Issue #740에서 `/api/payable`과 `/api/purchase` 별칭으로 매핑되었다. Receivable의 GET 목록/단건 2개는 여전히 MISSING이다. 아래 Payable 비교는 이제 실제 GET의 `PurchaseInvoiceResponse`, Receivable 비교는 후보 POST 응답을 기준으로 한다.

근거: `frontend/src/services/payableService.ts:1`, `receivableService.ts:1`, `PD/PurchaseInvoiceResponse.java:14`, `SD/SalesInvoiceResponse.java:8`.

| 필드(전체) | FE PayableInvoice / ReceivableInvoice | 후보 POST 응답 / 판정 |
|---|---|---|
| id | required number 둘 다 | Long |
| invoiceNo | required string 둘 다 | String |
| issueDate, dueDate | required string 각각, 둘 다 | LocalDate 각각 |
| totalAmount | required number 둘 다 | BigDecimal |
| businessPartnerCode | required string 둘 다 | payable vendorCode:String / receivable customerCode:String: 이름 차이 |
| paidAmount / collectedAmount | required number, 각 서비스 | 없음 |
| balanceAmount | required number 둘 다 | 없음 |
| accountCode, departmentCode, currencyCode | required string 각각, 둘 다 | 없음 |
| status (payable) | ISSUED/PARTIALLY_PAID/FULLY_PAID/CANCELLED | RECEIVED/APPROVED/PAID/PARTIAL_PAID/OVERDUE/CANCELLED; CANCELLED만 공통 |
| status (receivable) | ISSUED/PARTIALLY_COLLECTED/FULLY_COLLECTED/DEFAULTED | ISSUED/PAID/PARTIAL_PAID/OVERDUE/CANCELLED; ISSUED만 공통 |
| description | payable optional string / receivable 없음 | nullable String 둘 다 |
| netAmount, taxAmount | 없음 둘 다 | BigDecimal 각각 |
| journalEntryId, createdBy | 없음 | payable만 nullable Long/String |

두 후보 DTO는 flat이다. enum 근거: `payable/core/src/main/java/com/ho/account/expenditure/domain/PurchaseInvoiceStatus.java:6`, `receivable/core/src/main/java/com/ho/account/receivable/domain/SalesInvoiceStatus.java:6`. nullable 설명 근거: 각 core `infrastructure/persistence/entity/PurchaseInvoiceJpaEntity.java:24,49`, `SalesInvoiceJpaEntity.java:21,47`.

두 목록 함수는 optional truthy status를 URLSearchParams로 전송하지만 HTTP/transport/JSON 오류면 status를 무시하는 고정 mock 2건을 반환한다. 단건은 non-OK throw, network/JSON 오류 전파, mock 없음이다. 성공 JSON은 shape 검사 없이 반환한다.

미사용 POST 입력(`PD/PurchaseInvoiceRequest.java:18`, `SD/SalesInvoiceRequest.java:17`)은 invoiceNo, vendorCode 또는 customerCode, createdBy: nonblank String; issueDate/dueDate: required LocalDate; totalAmount/netAmount: required BigDecimal ≥.01; taxAmount: required BigDecimal ≥0; description: optional nullable String이다. dueDate≥issueDate 및 total=net+tax 제약이 있다. 대응 FE create 요청 자체가 없으므로 이 계약의 FE 일치를 주장하지 않는다.

### taxService

6개 method/path는 APInvoiceController에 존재한다. GET 목록의 startDate/endDate는 required ISO LocalDate, id는 Long, issue-id는 String이다. FE issueId를 URL encode 없이 삽입하므로 예약 문자가 경로를 바꿀 수 있다.

근거: `frontend/src/services/taxService.ts:6,18`, `TD/TaxInvoiceDto.java:14,52`, `TD/TaxInvoiceRequestDto.java:20`.

| 필드(전체) | FE request / response | BE / 차이 |
|---|---|---|
| id | response required number, path number | Long, generated ID |
| issueId | required string 둘 다 | request @NotBlank String / response String |
| type | required SALES/PURCHASE 둘 다 | request String @NotBlank @Pattern(SALES or PURCHASE), response String; **실제 AP core PURCHASE만 허용** |
| issueDate | required string 둘 다 | request @NotNull LocalDate / response LocalDate |
| businessPartnerCode | required string 둘 다 | request @NotBlank String, core partner 존재 확인 / response String |
| businessPartnerName | response required string | **mapper가 항상 null을 전달** (`TaxInvoiceDto.java:52`): DRIFT |
| supplyAmount, taxAmount, totalAmount | required number 각각, 둘 다 | request @NotNull @PositiveOrZero BigDecimal / response BigDecimal |
| status | 없음 | ACTIVE/CANCELLED enum |
| cancelledBy, cancellationReason | 없음 | nullable String 각각 |
| cancelledAt | 없음 | nullable LocalDateTime |

Request는 issueId/type/issueDate/businessPartnerCode/supplyAmount/taxAmount/totalAmount 7필드이고 nested 없음. 공급가액+세액=합계 제약이 domain에 있다. FE number는 음수/합계 불일치를 막지 않는다. `tax/core/src/main/java/com/ho/account/tax/application/service/TaxInvoiceService.java:40,59,66,73,80`의 create/update는 SALES를 거부하며 모든 read는 PURCHASE만 반환한다. DTO annotation의 SALES 허용과 실제 유즈케이스를 구분해야 한다. 상태/취소 필드 추가는 FE가 소비하지 않으며, businessPartnerName의 null은 확정 drift다.

DELETE `taxService.ts:98`는 `reason` query를 보내지 않는다. `tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java:78,81,82`는 required reason과 신뢰된 X-User-ID를 요구한다. 따라서 현재 함수로는 유효한 취소 입력을 만들 수 없다. 동작은 물리 삭제가 아닌 취소이며 성공 204/empty body를 FE Promise<void>가 parse하지 않는 것은 맞다. `tax/core/src/main/java/com/ho/account/tax/domain/TaxInvoice.java:126`은 nonblank actor/reason 및 중복 취소 방지를 검사한다.

목록은 HTTP/transport/JSON 오류 때 날짜를 무시하는 2개 mock(PURCHASE 1개, SALES 1개)을 반환하므로 AP core 제한도 가린다. 단건/issue-id/create/update/delete는 오류를 throw하며 mock이 없다.

## assetService / leaseService / bankingService / fxService DTO 대조

경로 약어: `AX=asset-lease/core/src/main/java/com/ho/account/asset`, `AW=asset-lease/api/src/main/java/com/ho/account/asset/web`. 자산·리스 응답은 별도 Response DTO가 아닌 Lombok getter를 가진 domain entity다. 이 감사에서 계층 리팩터링을 수행하지 않는다.

### assetService

Request 근거: `frontend/src/services/assetService.ts:26`, `AX/dto/FixedAssetRequest.java:10`. 11필드 이름은 일치한다.

| Request 필드 | FE | BE |
|---|---|---|
| assetCode, assetName | required string 각각 | @NotBlank String 각각 |
| accountSubjectCode | required string | @NotBlank String; Controller :46에서 response accountCode로 변환 |
| accumulatedAccountCode, expenseAccountCode | required string 각각 | @NotBlank String 각각 |
| acquisitionDate | required string | @NotNull LocalDate |
| acquisitionCost | required number | @NotNull BigDecimal ≥.01 |
| usefulLife | required number | @NotNull Integer ≥1 |
| depreciationMethod | required string | @NotBlank String; enum 아님 (주석 STRAIGHT_LINE/DECLINING) |
| residualValue | required number | @NotNull BigDecimal ≥0 |
| departmentCode | required string | @NotBlank String |

dispose body `assetService.ts:98,102` ↔ `AX/dto/FixedAssetDisposalRequest.java:8`: `assetId:number`→@NotNull Long(양수 annotation 없음), `disposalDate:string`→@NotNull LocalDate, `salePrice:number`→@NotNull BigDecimal ≥0. 중첩 없음. Depreciation body는 없고 path processDate가 ISO LocalDate, 응답은 plain text를 `response.text()`로 읽는다.

Response 근거: `frontend/src/services/assetService.ts:6`, `AX/domain/FixedAsset.java:36`.

| Response 필드(전체) | FE | BE / 차이 |
|---|---|---|
| id | number | generated Long |
| assetCode, assetName, accountCode | string 각각 | DB non-null String 각각 |
| accumulatedAccountCode, expenseAccountCode | string 각각 | nullable column String 각각 |
| acquisitionDate | string | DB non-null LocalDate |
| acquisitionCost | number | DB non-null BigDecimal(19,2) |
| usefulLife | number | primitive int |
| depreciationMethod | string | nullable String |
| residualValue, accumulatedDepreciation | number 각각 | nullable BigDecimal(19,2), default ZERO |
| currentBookValue | number | DB non-null BigDecimal(19,2) |
| depreciationAmountPerPeriod | number | nullable BigDecimal(19,2), **default 없음, 등록 시에도 미설정** |
| lastDepreciationDate | string 또는 null | nullable LocalDate; null 대응 |
| status | ACTIVE/DISPOSED/FULLY_DEPRECIATED | DB non-null String; 업무 값 대응하나 enum 강제 아님 |
| departmentCode | string | nullable String |
| createdAt, updatedAt | 없음 | LocalDateTime 각각; lifecycle에서 설정 |

nullable column은 매 응답이 null이라는 뜻이 아니다. 그러나 `AX/application/service/FixedAssetEntryService.java:44`의 등록은 취득잔액/ACTIVE/save만 초기화하고 depreciationAmountPerPeriod를 설정하지 않아 required number와 충돌한다. 응답에 nested/inheritance 필드는 없다.

getAssets의 status 누락/blank는 core에서 ACTIVE, 기타 값은 trim/uppercase로 해석한다(`FixedAssetEntryService.java:130`). 목록은 HTTP/transport/JSON 오류 때 status 필터 없이 mock 2건을 반환한다. 나머지 4개 함수는 오류를 throw하고 mock이 없다.

### leaseService

Request 근거: `frontend/src/services/leaseService.ts:26`, `AX/dto/LeaseContractRequest.java:14`. 16필드, nested 없음.

| Request 필드 | FE | BE |
|---|---|---|
| contractNo, contractName | required string 각각 | @NotBlank String 각각 |
| lessorBusinessPartnerCode | required string | nullable String; Controller :39에서 response lessorCode로 변환 |
| startDate, endDate | required string 각각 | @NotNull LocalDate 각각 |
| monthlyPayment | required number | @NotNull BigDecimal; DTO 양수 제약 없음 |
| paymentDay | required number | int @Min(1) @Max(31) |
| discountRate | required number | @NotNull BigDecimal; DTO 양수 제약 없음 |
| initialRightOfUseAssetValue, initialLeaseLiabilityValue | required number 각각 | nullable BigDecimal 각각 |
| status | optional string | nullable String; null이면 Controller ACTIVE 기본값 |
| departmentCode, expenseAccountCode | required string 각각 | nullable String 각각 |
| ifrs16Applicable | optional boolean | primitive boolean default true |
| shortTermLease, lowValueLease | optional boolean 각각 | primitive boolean default false 각각 |

`AX/domain/LeaseContract.java:92`는 지급일/날짜/start≤end를 검사한다. IFRS16 적용·비단기·비소액 계약의 initial PV는 `:164`에서 재계산 값으로 대체한다(`AX/application/service/LeaseEntryService.java:48`). FE 필드가 required여도 그대로 저장되는 값임을 뜻하지 않는다.

Response 근거: `frontend/src/services/leaseService.ts:6`, `AX/domain/LeaseContract.java:46`. 17필드이며 ROU/liability/schedule nested 객체를 응답에 추정 추가하지 않는다.

| Response 필드 | FE | BE / 차이 |
|---|---|---|
| id | number | generated Long |
| contractNo, contractName | string 각각 | DB non-null String 각각 |
| lessorCode | string | nullable String |
| startDate, endDate | string 각각 | DB non-null LocalDate 각각 |
| monthlyPayment | number | DB non-null BigDecimal(19,2) |
| paymentDay | number | primitive int |
| discountRate | number | DB non-null BigDecimal(7,4) |
| initialRightOfUseAssetValue, initialLeaseLiabilityValue | number 각각 | nullable BigDecimal(19,2) 각각 |
| status | ACTIVE/TERMINATED/MODIFIED | nullable 임의 String; 요청에서도 enum 제한 없음 |
| departmentCode, expenseAccountCode | string 각각 | nullable String 각각 |
| ifrs16Applicable | boolean | primitive boolean default true |
| shortTermLease, lowValueLease | boolean 각각 | primitive boolean default false 각각 |

getLeases는 ACTIVE 목록만 조회한다(`LeaseEntryService.java:150`). HTTP/transport/JSON 오류면 mock 2건을 반환한다. 나머지 함수는 오류를 throw한다. processMonthly는 String path를 BE 내부 LocalDate.parse로 해석하며 성공 plain text, parse/처리 예외는 500 text다.

자산 3개·리스 2개 mutation은 BE에서 X-User-ID 필수다(`AW/FixedAssetController.java:35,58,65`, `AW/LeaseAccountingController.java:28,69`). FE 함수가 직접 넣지 않아도 인증된 BFF→Gateway actor 주입 경로와 함께 봐야 하며, 헤더 미기재만으로 통합 실패라고 단정하지 않는다. 직접 호출이라면 필수다.

미사용 remeasure POST의 `AX/dto/LeaseRemeasurementRequest.java:8`: contractId:@NotNull Long, remeasurementDate:@NotNull LocalDate, newMonthlyPayment:nullable BigDecimal(있으면 ≥.01), newEndDate:nullable LocalDate, newDiscountRate:nullable BigDecimal(있으면 ≥0). 성공 LeaseContract, IllegalArgumentException 400 empty, 기타 Exception 500 empty다. 대응 FE 함수 없음.

### bankingService / fxService: 백엔드 계약 없음

3개 URL/method 모두 MISSING이다. 비슷한 Treasury/FX 유즈케이스나 Controller 이름을 추정해 매핑하지 않는다. 모든 기대 필드가 required/non-null이고 비교할 endpoint DTO가 없다.

| FE 계약 / 근거 | 필드 전수 |
|---|---|
| bankingService.ts:10 InterBranchDashboardData | unmatchedCount:number, totalDiscrepancyAmount:number, autoMatchRate:number, unexplainedDepositsCount:number, transactions:InterBranchTransaction[] |
| bankingService.ts:1 InterBranchTransaction | id:number, sourceBranch:string, targetBranch:string, transactionType:string, amount:number, status:MATCHED/DISCREPANCY/PENDING |
| fxService.ts:18 FxDashboardData | totalNetPosition:number, totalKrwAmount:number, dailyValuationGainLoss:number, gainLossPercent:number, rates:FxRate[], positions:FxPosition[] |
| fxService.ts:11 FxRate | pair:string, rate:number, changeAmount:number, changePercent:number |
| fxService.ts:1 FxPosition | currencyCode:string, currencyName:string, foreignAmount:number, averageRate:number, krwAmount:number, valuationGainLoss:number, limitStatus:SAFE/WARNING/EXCEEDED |

두 dashboard는 HTTP/transport 오류면 mock을 반환하지만 `return response.json()`을 await하지 않아 비동기 JSON 오류는 전파한다. banking mock은 transaction2건, FX mock은 rates2건/positions2건이다. auto-match는 body를 parse하지 않고 response.ok→boolean, 예외→false다. 성공 boolean이 server response DTO는 아니다.

## Backend-only mapping 부록

아래는 기준 커밋 `34c75839` 당시 대응 Controller 13개의 전체 87개 mapping 중 service에서 사용하지 않던 54개의 역사적 부록이다. Issue #740의 신규/별칭 route를 다시 열거하는 현재 inventory가 아니다.

| Controller / 근거 | Method / class+method 경로 |
|---|---|
| [asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java:79](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/asset-lease/api/src/main/java/com/ho/account/asset/web/LeaseAccountingController.java#L79) | `POST /api/ifrs16/leases/remeasure` |
| [auth/api/src/main/java/com/ho/account/auth/api/web/AuthController.java:56](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/auth/api/src/main/java/com/ho/account/auth/api/web/AuthController.java#L56) | `POST /api/auth/validate-token-version` |
| [auth/api/src/main/java/com/ho/account/auth/api/web/AuthController.java:64](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/auth/api/src/main/java/com/ho/account/auth/api/web/AuthController.java#L64) | `POST /api/auth/internal/users/{username}/role-assignments` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:41](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L41) | `POST /api/closing/calendars` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:53](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L53) | `GET /api/closing/calendars/{id}` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:65](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L65) | `GET /api/closing/calendars/by-period` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:77](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L77) | `PUT /api/closing/calendars/{id}/status` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:90](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L90) | `POST /api/closing/tasks` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:118](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L118) | `POST /api/closing/gates` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:133](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L133) | `PUT /api/closing/gates/{id}/check` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:146](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L146) | `POST /api/closing/period-locks` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:158](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L158) | `DELETE /api/closing/period-locks/{fiscalPeriodId}` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:171](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L171) | `POST /api/closing/reopen-approvals` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:183](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L183) | `PUT /api/closing/reopen-approvals/{id}/status` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:222](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L222) | `POST /api/closing/adjustments` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:241](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L241) | `POST /api/closing/calendars/determine-status` |
| [closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java:250](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/closing/api/src/main/java/com/ho/account/closing/web/ClosingController.java#L250) | `POST /api/closing/annual/perform-income-statement-closing` |
| [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java:47](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java#L47) | `POST /api/journals/from-event` |
| [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java:77](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java#L77) | `GET /api/journals/{slipNo}` |
| [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java:88](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java#L88) | `POST /api/journals/{id}/approve` |
| [journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java:103](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalController.java#L103) | `POST /api/journals/{id}/post` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java:22](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java#L22) | `POST /api/basic/account-subjects` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java:39](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java#L39) | `GET /api/basic/account-subjects/{code}` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java:46](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java#L46) | `PUT /api/basic/account-subjects/{code}` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java:56](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/AccountSubjectController.java#L56) | `DELETE /api/basic/account-subjects/{code}` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:31](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L31) | `POST /api/basic/businesspartners` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:54](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L54) | `GET /api/basic/businesspartners/active` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:64](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L64) | `GET /api/basic/businesspartners/{businessPartnerCode}` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:75](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L75) | `GET /api/basic/businesspartners/search` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:85](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L85) | `PUT /api/basic/businesspartners/{id}` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java:99](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/BusinessPartnerController.java#L99) | `DELETE /api/basic/businesspartners/{id}` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:89](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L89) | `POST /api/master-data/change-requests/{requestId}/apply` |
| [master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java:96](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataChangeRequestController.java#L96) | `POST /api/master-data/change-requests/apply-due` |
| [payable/api/src/main/java/com/ho/account/expenditure/payable/api/adapter/in/web/PurchaseController.java:38](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/payable/api/src/main/java/com/ho/account/expenditure/payable/api/adapter/in/web/PurchaseController.java#L38) | `POST /api/purchase/invoices` |
| [payable/api/src/main/java/com/ho/account/expenditure/payable/api/adapter/in/web/PurchaseController.java:47](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/payable/api/src/main/java/com/ho/account/expenditure/payable/api/adapter/in/web/PurchaseController.java#L47) | `POST /api/purchase/payables/update-status/{asOfDate}` |
| [receivable/api/src/main/java/com/ho/account/receivable/api/adapter/in/web/SalesController.java:39](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/receivable/api/src/main/java/com/ho/account/receivable/api/adapter/in/web/SalesController.java#L39) | `POST /api/sales/invoices` |
| [receivable/api/src/main/java/com/ho/account/receivable/api/adapter/in/web/SalesController.java:45](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/receivable/api/src/main/java/com/ho/account/receivable/api/adapter/in/web/SalesController.java#L45) | `POST /api/sales/receivables/update-status/{asOfDate}` |
| [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:77](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L77) | `POST /api/v1/reporting/submissions/regulatory` |
| [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:124](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L124) | `POST /api/v1/reporting/regulatory-filings/submit` |
| [reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java:135](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/reporting/api/src/main/java/com/ho/account/reporting/adapter/in/web/ReportingController.java#L135) | `GET /api/v1/reporting/regulatory-filings/latest` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:36](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L36) | `GET /api/audit/logs` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:45](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L45) | `GET /api/audit/logs/user/{userId}` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:52](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L52) | `GET /api/audit/logs/target` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:60](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L60) | `GET /api/audit/logs/type/{eventType}` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:69](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L69) | `GET /api/audit/roles` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:76](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L76) | `GET /api/audit/roles/{roleCode}` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:81](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L81) | `POST /api/audit/roles` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:92](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L92) | `GET /api/audit/roles/{roleCode}/authorizations` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:99](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L99) | `POST /api/audit/roles/{roleCode}/authorizations` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:111](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L111) | `DELETE /api/audit/authorizations/{authorizationId}` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:117](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L117) | `GET /api/audit/permissions/check` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:127](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L127) | `GET /api/audit/approvals/pending` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:147](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L147) | `POST /api/audit/approvals/{approvalId}/approve` |
| [shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java:158](https://github.com/skyg547/account/blob/34c75839af2edf10f80ff60d8f24e89504d69e9e/shared-kernel/src/main/java/com/ho/account/shared/infrastructure/security/web/AuditController.java#L158) | `POST /api/audit/approvals/{approvalId}/reject` |

## 기준 감사 재실행 및 검증 증거 (2026-09-10)

다음 Node 스크립트와 결과는 기준 커밋 감사 당시의 재현 기록이다. Issue #740 이후 `@RequestMapping` 배열 별칭과 세 신규 GET이 추가되었으므로 현재 HEAD 검증 명령으로 사용하지 않는다. 현재 변경은 Closing/Payable Gradle controller 테스트와 표의 source link로 검증한다.

이 extractor는 현재 저장소의 직접 함수 호출/상수 URL 및 annotation 형태에 한정한다. 모든 Java 문법/동적 mapping/전역 Jackson 설정의 일반 검증기는 아니다. DTO nullable/enum/중첩 필드 전체 비교는 앞 절의 수동 소스 감사가 담당한다. 새 간접 helper·annotation 형태·serializer·DTO 변경 시 소스 감사를 다시 해야 한다.

저장소 루트에서 아래 javascript block을 `/tmp/account-179-verify.cjs`로 저장한 뒤 `node /tmp/account-179-verify.cjs`를 실행한다. 기존 `frontend/node_modules/typescript`가 필요하며 설치나 API 호출은 하지 않는다.

```javascript
// Run from repository root, after extracting this block to /tmp/account-179-verify.cjs.
const fs = require('node:fs');
const cp = require('node:child_process');
const assert = require('node:assert/strict');
const ts = require(process.cwd() + '/frontend/node_modules/typescript');
const doc = fs.readFileSync('frontend/docs/frontend-backend-api-parity-matrix.md', 'utf8');
const files = fs.readdirSync('frontend/src/services').filter(f => f.endsWith('.ts')).sort();
assert.equal(files.length, 15, 'Reaudit added/removed service files');
const expectedCounts = {adminService:2,apiClient:0,assetService:5,authService:1,bankingService:2,browserSession:0,closingService:4,fxService:1,journalService:2,leaseService:4,masterDataService:6,payableService:2,receivableService:2,reportingService:5,taxService:6};
const rows = [], helpers = [];
for (const file of files) {
  const source = fs.readFileSync('frontend/src/services/' + file, 'utf8');
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true);
  const constant = source.match(/const API_BASE_URL = '([^']+)'/);
  function visit(n) {
    if (ts.isCallExpression(n) && ['fetch','fetchWithTimeout','requestJson','requestJsonWithTimeout'].includes(n.expression.getText(ast))) {
      const name = n.expression.getText(ast), line = ast.getLineAndCharacterOfPosition(n.getStart(ast)).line + 1;
      if (file === 'apiClient.ts' || (file === 'masterDataService.ts' && name === 'fetch')) helpers.push(file + ':' + line);
      else {
        let method = 'GET', url = n.arguments[0].getText(ast);
        const init = n.arguments[1];
        if (init) {
          assert(ts.isObjectLiteralExpression(init), 'Reaudit indirect request options');
          for (const prop of init.properties) {
            assert(!ts.isSpreadAssignment(prop), 'Reaudit spread request options');
            if (ts.isPropertyAssignment(prop) && prop.name.getText(ast) === 'method') method = prop.initializer.text;
          }
        }
        if (constant) url = url.replaceAll('${API_BASE_URL}', constant[1]).replaceAll('API_BASE_URL', constant[1]);
        url = url.replace(/^[`'"]|[`'"]$/g, '').replace(/\$\{params \?.*/, '?status={status?}');
        url = url.replace('${params.toString()}', '{query}').replace(/\$\{([^}]+)\}/g, '{$1}');
        if (file === 'taxService.ts' && line === 37) url = url.replace('{query}', 'startDate={startDate}&endDate={endDate}');
        if (file === 'reportingService.ts') url = url.replace('{query}', 'type={type}&baseDate={baseDate}' + (line === 72 ? '&format={format}' : line === 113 ? '&entryId={entryId}' : ''));
        assert(url.startsWith('/api/'), 'Reaudit unresolved URL ' + url);
        rows.push({id:file.slice(0,-3)+':'+line, file, line, method, url});
      }
    }
    ts.forEachChild(n, visit);
  }
  visit(ast);
  assert.equal(rows.filter(r=>r.file===file).length, expectedCounts[file.slice(0,-3)], file);
}
assert.equal(helpers.length, 3);
const documented = [...doc.split('## 재실행')[0].matchAll(/<!-- endpoint:([^ ]+) -->/g)].map(m=>m[1]);
assert.deepEqual([...documented].sort(), rows.map(r=>r.id).sort(), 'Endpoint row omission/duplication');
const tracked = cp.execFileSync('git', ['ls-files','*.java'], {encoding:'utf8'}).trim().split('\n');
const mappings=[];
for (const file of tracked.filter(f=>f.includes('/src/main/java/') && f.endsWith('Controller.java'))) {
  const source=fs.readFileSync(file,'utf8'), cls=/\bclass\s+(\w+)/.exec(source);
  if (!cls) continue;
  const base=/@RequestMapping\s*\(\s*"([^"]+)"/.exec(source.slice(0,cls.index));
  const offset=cls.index+cls[0].length;
  for (const m of source.slice(offset).matchAll(/@(Get|Post|Put|Delete|Patch)Mapping(?:\s*\((.*?)\))?/gs)) {
    const suffix=/"([^"]*)"/.exec(m[2]||'');
    mappings.push({file, line:source.slice(0,offset+m.index).split('\n').length, method:m[1].toUpperCase(), path:(base?.[1]||'')+(suffix?.[1]||'')});
  }
}
const normalize=s=>s.replace(/\{[^}]*\}/g,'{}');
let mapped=0, missing=0;
for (const row of rows) {
  const line=doc.split('\n').find(s=>s.includes('<!-- endpoint:'+row.id+' -->'));
  assert(line.includes('`'+row.method+' '+row.url+'`'), 'Document URL/method drift: '+row.id);
  const matches=mappings.filter(m=>m.method===row.method && normalize(m.path)===normalize(row.url.split('?')[0]));
  assert(matches.length<=1, 'Ambiguous Controller mapping: '+row.id);
  if (matches.length) {
    mapped++;
    assert(line.includes('/'+matches[0].file+'#L'+matches[0].line), 'Document Controller evidence drift: '+row.id);
    assert(line.includes('MAPPED') && !line.includes('MISSING'));
  } else { missing++; assert(line.includes('MISSING'), 'Unreported missing endpoint: '+row.id); }
}
assert.equal(mapped,33); assert.equal(missing,9);
const controllerFiles = new Set(rows.flatMap(r=>mappings.filter(m=>m.method===r.method && normalize(m.path)===normalize(r.url.split('?')[0])).map(m=>m.file)));
for (const m of mappings) if (/(PurchaseController|SalesController)\.java$/.test(m.file)) controllerFiles.add(m.file);
const extras=mappings.filter(m=>controllerFiles.has(m.file) && !rows.some(r=>r.method===m.method && normalize(r.url.split('?')[0])===normalize(m.path)));
assert.equal(controllerFiles.size,13); assert.equal(extras.length,54);
const appendix=doc.split('## Backend-only mapping 부록')[1]?.split('## 재실행')[0] || '';
const appendixRows=appendix.split('\n').filter(l=>l.startsWith('| [')).length;
assert.equal(appendixRows,54,'Backend-only row count drift');
for (const m of extras) assert(appendix.includes('/'+m.file+'#L'+m.line) && appendix.includes('`'+m.method+' '+m.path+'`'),'Backend-only mapping drift');

// Regression sentinels read actual DTOs: these are intentionally detected mismatches,
// not compatibility assertions. Full nested/nullable/enum comparison is the manual audit above.
const journal=fs.readFileSync('journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalApiDto.java','utf8');
const journalFe=fs.readFileSync('frontend/src/services/journalService.ts','utf8');
assert(/details:\s*JournalDetailDto\[\]/.test(journalFe) && /List<@Valid LineRequest> lines/.test(journal));
assert(!/baseAmount\??:/.test(journalFe) && /BigDecimal baseAmount/.test(journal));
const master=fs.readFileSync('master-data/api/src/main/java/com/ho/account/masterdata/api/dto/AccountSubjectDto.java','utf8');
assert(!/private\s+\w+\s+(type|status)\s*;/.test(master));
const tax=fs.readFileSync('tax/api/src/main/java/com/ho/account/tax/api/adapter/in/web/APInvoiceController.java','utf8');
assert(/@RequestParam String reason/.test(tax));
assert(!/reason/.test(fs.readFileSync('frontend/src/services/taxService.ts','utf8')));
assert(!/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]/.test(doc), 'Control character');
assert(!/^(<<<<<<<|=======|>>>>>>>)/m.test(doc), 'Conflict marker');
console.log('PASS: 15 files, 42 endpoint rows, 3 helpers, 33 mapped, 9 MISSING; known DTO/query drift detected; Markdown controls clean.');
```

### 검사기 실패 사례 검증

아래 두 번째 javascript block을 `/tmp/account-179-negative.cjs`로 저장한 뒤 실행한다. 첫 번째 스크립트가 `/tmp/account-179-verify.cjs`에 있어야 한다. 실제 파일을 변경하지 않고 읽기 결과만 메모리에서 변형하여 각 오류를 검출하는지 확인한다.

```javascript
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const code=fs.readFileSync('/tmp/account-179-verify.cjs','utf8');
const cases=[
 ['missing row','frontend/docs/frontend-backend-api-parity-matrix.md',s=>s.replace(/<!-- endpoint:authService:21 -->/,'')],
 ['wrong method','frontend/docs/frontend-backend-api-parity-matrix.md',s=>s.replace('`POST /api/auth/login`','`GET /api/auth/login`')],
 ['wrong prefix','frontend/docs/frontend-backend-api-parity-matrix.md',s=>s.replace('`POST /api/auth/login`','`POST /api/v1/auth/login`')],
 ['wrong controller evidence','frontend/docs/frontend-backend-api-parity-matrix.md',s=>s.replace('AuthController.java#L49','AuthController.java#L999')],
 ['backend-only missing row','frontend/docs/frontend-backend-api-parity-matrix.md',s=>s.replace(/\| \[auth\/.*?validate-token-version.*\n/,'')],
 ['nested DTO drift','journal-ledger/api/src/main/java/com/ho/account/journalledger/adapter/in/web/journal/JournalApiDto.java',s=>s.replaceAll('BigDecimal baseAmount','BigDecimal renamedAmount')],
 ['control character','frontend/docs/frontend-backend-api-parity-matrix.md',s=>s+'\u0007'],
];
for(const [name,file,mutate] of cases){
 let changed=false;
 const fakeFs={...fs,readFileSync(p,...args){const value=fs.readFileSync(p,...args);if(String(p)===file){const next=mutate(value);changed=next!==value;return next;}return value;}};
 let failure;
 try {vm.runInNewContext(code,{require:n=>n==='node:fs'?fakeFs:require(n),process,console:{log(){}}});}catch(e){failure=e;}
 assert(changed,name+' fixture failed to change source');
 assert(failure?.code==='ERR_ASSERTION',name+' was not detected: '+failure);
 console.log('PASS negative:',name);
}
```

### 실행 결과 (2026-09-10)

| 명령 / 검사 | 결과 |
|---|---|
| `node /tmp/account-179-verify.cjs` (루트) | PASS: 15 files, 42 endpoint rows, 3 helpers, 33 mapped, 9 MISSING; known DTO/query drift detected; Markdown controls clean. 대응 13 Controller의 87 mapping 중 backend-only 54개 행도 일치 |
| `node /tmp/account-179-negative.cjs` (루트) | PASS 7/7: row 누락, method 오류, /api↔/api/v1 오류, Controller link 오류, backend-only 행 누락, nested DTO drift, 제어문자 |
| `node frontend/node_modules/typescript/bin/tsc --project frontend/tsconfig.json --noEmit --incremental false` (루트) | exit 0, diagnostics 없음 |
| `NEXT_TELEMETRY_DISABLED=1 NODE_OPTIONS=--max-old-space-size=3072 taskset -c 0 npm run build` (`frontend/`) | exit 0; Next 15.5.23 compiled successfully, type validation, static pages 122/122 |
| `git diff --check`; tracked source/docs conflict scan; 새 문서 및 추가/변경 줄 control-character scan | PASS |
| 독립 read-only Reviewer | PUT JSON parse 실패를 null로 잘못 일반화한 P3 수정 후 재검토: 남은 P0-P3 없음 |

기존 `/home/ho/dev/account/frontend/node_modules`를 격리 worktree의 ignored symlink로 재사용했다. 설치/lockfile 변경은 없다. 핵심 설치 버전 Next 15.5.23, TypeScript 5.9.3, React/ReactDOM 19.2.8은 lockfile과 일치한다. 실행 Node는 **22.23.2**, npm 10.9.8이다. 저장소 engines의 Node20 검증으로 주장하지 않으며, Node20 재실행은 별도 환경에서 필요하다.

기존 `docs/ai-harness/worklog.md`에 있던 제어문자 1개는 기준 HEAD와 동일하게 보존했다. 새 문서와 추가/변경 내용의 제어문자 검사는 통과했다.

백엔드 production 코드 변경이 없으므로 Gradle 빌드/테스트는 실행하지 않았다. 실제 Gateway/DB/로그인/금융 API 호출, Jackson 직렬화 smoke, 전체 UI browser smoke는 미실행이다. build/typecheck는 서버 계약 호환성 증명이 아니며 위 정적 감사의 실제 불일치를 해결하지 않는다.

## 후속 수정과 인수 기준

- MISSING 6개: admin 사용자 목록 1, banking 2, fx dashboard 1, receivable 2. 사용자 기능 요구와 맞는 backend 계약부터 별도 변경 범위로 합의해야 한다. Closing 1개와 Payable 2개는 Issue #740에서 매핑되었다.
- 전표 query와 lines/baseAmount/description, 계정과목 category/type/status, tax reason/PURCHASE/null name, nullable asset/lease/partner 응답, reporting PDF 파일명 등의 DRIFT를 후속 production 변경에서 해결해야 한다.
- Audit 승인 Gateway 노출과 사용자명/actor 의미, Closing의 `isMandatory`(FE) 대 `mandatory`(BE) 필드명 DRIFT, live 통합 동작을 별도 검증해야 한다.
- 이번 이슈의 전수 감사 결과는 source 근거와 함께 제출한다. '모든 endpoint가 존재하고 모든 DTO가 호환됨'은 **충족하지 않으므로 이슈를 자동 종료하지 않는다**. 리뷰어는 감사의 완전성과 후속 수정 범위를 판단한다.
- 롤백은 이 문서/README/harness 변경 커밋 revert. API/DB migration/배포 롤백은 발생하지 않는다.
