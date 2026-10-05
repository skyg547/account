# 📑 Asset-Lease API Specification

이 문서는 고정자산 관리 및 IFRS 16 리스 회계 모듈의 REST API 명세서입니다. 모든 금액 계산은 `BigDecimal`을 사용하여 정밀도를 보장하며, 헥사고날 아키텍처 원칙에 따라 설계되었습니다.
상세 업무 흐름은 [process-flow.md](./process-flow.md), 로컬 실행 방법은 [local-run.md](./local-run.md)를 함께 확인하세요.

---

## 1. 고정자산 관리 (Fixed Asset Management)
고정자산의 취득, 감가상각, 처분 및 이력 관리를 담당합니다.

### [POST] 고정자산 등록 및 초기 인식
- **URL:** `/api/fixed-assets`
- **목적:** 새로운 자산을 등록하고 시스템에 초기 취득 원가를 반영합니다. (회계 전표 생성 이벤트 포함)
- **Required Header:** `X-User-ID` (자산 이력과 이벤트에 기록되는 실행자)
- **Request Body (FixedAssetRequest):**
  - `assetCode` (String): 자산 고유 코드
  - `assetName` (String): 자산 명칭
  - `accountSubjectCode` (String): 자산 계정 과목 코드
  - `acquisitionDate` (LocalDate): 취득 일자
  - `acquisitionCost` (BigDecimal): 취득 원가
  - `usefulLife` (int): 내용 연수
  - `depreciationMethod` (String): 상각 방법 (STRAIGHT_LINE 등)
  - `accumulatedAccountCode` (String): 감가상각누계액 계정 코드
  - `expenseAccountCode` (String): 감가상각비 계정 코드
  - `residualValue` (BigDecimal): 잔존가치
  - `departmentCode` (String): 관리 부서 코드
- **Success Response:** `201 Created` (등록된 FixedAsset 엔티티)

### [POST] 월별 감가상각 실행
- **URL:** `/api/fixed-assets/depreciate/{processDate}`
- **목적:** 특정 날짜 기준으로 감가상각비를 산출하고 자산의 장부 가액을 갱신합니다.
- **Required Header:** `X-User-ID`
- **Path Variable:** `processDate` (LocalDate, e.g., 2026-04-30)
- **Success Response:** `200 OK`

### [POST] 고정자산 처분
- **URL:** `/api/fixed-assets/dispose`
- **목적:** 자산을 매각하거나 폐기하여 장부에서 제거하고 처분 손익을 계산합니다.
- **Required Header:** `X-User-ID`
- **Request Body (FixedAssetDisposalRequest):**
  - `assetId` (Long): 대상 자산 ID
  - `disposalDate` (LocalDate): 처분 일자
  - `salePrice` (BigDecimal): 매각 금액 (폐기 시 0)
- **Success Response:** `200 OK` (처분 처리된 FixedAsset 엔티티)

---

## 2. IFRS 16 리스 회계 (Lease Accounting)
리스 계약에 기반한 사용권자산(ROU) 및 리스부채의 현재가치(PV) 측정과 상환 스케줄링을 담당합니다.

### [POST] 리스 계약 등록 및 초기 인식
- **URL:** `/api/ifrs16/leases`
- **목적:** 리스 계약 정보를 입력받아 사용권자산과 리스부채의 PV를 측정하고 상환 스케줄을 자동 생성합니다.
- **Request Body (LeaseContractRequest):**
  - `contractNo` (String): 리스 계약 번호
  - `startDate` / `endDate` (LocalDate): 리스 기간
  - `monthlyPayment` (BigDecimal): 월 리스료
  - `paymentDay` (int): 매월 지급일
  - `discountRate` (BigDecimal): 증분차입이자율 (%)
  - `ifrs16Applicable` (boolean): IFRS 16 적용 대상 여부
  - `shortTermLease` / `lowValueLease` (boolean): 단기/소액 리스 예외 여부
  - `initialRightOfUseAssetValue` / `initialLeaseLiabilityValue` (BigDecimal): 최초 사용권자산/리스부채 인식 금액
  - `lessorBusinessPartnerCode`, `departmentCode`, `expenseAccountCode`: 거래처/부서/비용 계정 코드
- **Success Response:** `201 Created` (생성된 LeaseContract 엔티티 및 스케줄)

### [GET] 리스 계약 목록 조회
- **URL:** `/api/ifrs16/leases`
- **목적:** 등록된 모든 리스 계약의 현황을 조회합니다.
- **Success Response:** `200 OK` (List<LeaseContract>)

### [POST] 리스 조건 변경 및 재측정
- **URL:** `/api/ifrs16/leases/remeasure`
- **목적:** 리스 기간 연장, 리스료 변경 등 계약 조건 변경 시 리스부채와 사용권자산을 재측정합니다.
- **Request Body (LeaseRemeasurementRequest):**
  - `contractId` (Long): 대상 계약 ID
  - `remeasurementDate` (LocalDate, 필수): 아직 `SCHEDULED`인 첫 회차의 날짜 (`YYYY-MM-DD`)
  - `newMonthlyPayment` (BigDecimal, 선택): 변경된 월 리스료, 양수·소수 둘째 자리까지
  - `newEndDate` (LocalDate, 선택): 변경된 종료일, 기준일 이후
  - `newDiscountRate` (BigDecimal, 선택): 변경된 연 이자율(%), 0 이상·소수 넷째 자리까지
- **Header:** `X-User-ID` (실행자, 필수)
- **예시:** `{"contractId":1,"remeasurementDate":"2026-06-01","newMonthlyPayment":2400.00}`
- **Success Response:** `200 OK` (조건이 반영된 계약 엔티티; 부채·사용권자산·상환표는 DB에 함께 저장)
- **검증:** 이미 `PAID`인 기준일이나 미처리 과거 회차, 계약 기간 밖 기준일, 잘못된 금액·이자율은 거절한다. 같은 조건과 기준일의 재시도는 미래 회차를 중복 생성하지 않는다.

## 3. 현재 구현 주의사항

- `asset-lease`는 `core`/`api`/`batch`로 분리되어 있으며, API는 `:asset-lease:api:bootRun`, Batch는 `:asset-lease:batch:bootRun`으로 실행합니다. 로컬에서는 Config Server/Eureka를 끄고 실행하는 것을 권장합니다.
- 고정자산 등록/상각/처분과 리스 등록/월별 회계처리/재측정 API는 `X-User-ID`를 받아 이력 또는 이벤트에 실행자를 남깁니다.
- 자산/리스 이벤트는 `transaction-events` Kafka 토픽으로 발행됩니다.
- 리스료 지급결의는 `LeasePaymentResolutionPort` 구현이 필요합니다. 단독 실행에서 구현이 없으면 fallback 어댑터가 예외를 던집니다.

---

## 💡 초보자를 위한 개념 설명
- **PV (Present Value, 현재가치):** 미래에 낼 리스료를 지금 시점의 가치로 환산한 금액입니다. 은행 이자를 생각하면 쉽습니다.
- **사용권자산(ROU Asset):** 리스 계약을 통해 자산을 사용할 수 있는 '권리'를 자산으로 잡는 것입니다.
- **리스부채(Lease Liability):** 앞으로 내야 할 리스료를 '빚'으로 잡는 것입니다.
