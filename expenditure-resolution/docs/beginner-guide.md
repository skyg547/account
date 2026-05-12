# expenditure-resolution 초보자 가이드 (Beginner Guide)

`expenditure-resolution` 모듈은 회사의 비용 지출에 대한 승인 문서(지출결의서)를 작성하고, 승인이 완료되면 회계 전표 및 지급 처리로 안전하게 연결하는 역할을 합니다. 이 모듈은 헥사고날 아키텍처를 기반으로 설계되었습니다.

## 🌟 초보자를 위한 개념 설명

* **지출결의서 (Expenditure Resolution):** 회사 돈을 쓰기 전에 "이 목적으로, 이 거래처에, 이만큼의 돈을 쓰겠습니다"라고 올리는 기안(결재) 문서입니다.
* **AP 지급 (Accounts Payable Payment):** 승인된 지출결의서에 따라 실제로 돈을 지급해야 할 내역(외상매입금/미지급금 등)을 관리하는 목록입니다. 결의가 나면 지급 대기 목록에 올라가게 됩니다.
* **예산 통제 (Budget Control):** 부서별, 계정과목별로 한 달에 쓸 수 있는 돈의 한도(예산)가 정해져 있습니다. 지출결의서를 작성할 때 예산이 남아있는지 확인하고, 작성하는 순간 남은 예산을 깎아놓는 통제 과정입니다.
* **ID 기반 참조:** 지출결의서는 결재를 올린 사람, 비용을 쓸 부서, 돈을 받을 거래처 등의 정보를 `master-data`에서 가져오지만, 직접 DB를 연결하지 않고 사번, 부서코드, 거래처코드(ID)만 기록해 둡니다.

## 핵심 도메인 개념

### 지출결의 (ExpenditureResolution) & 상세 (ExpenditureDetail)
- `ExpenditureResolution`은 결의서 전체를 대변하는 헤더(총액, 제목, 작성자 등)입니다.
- `ExpenditureDetail`은 차변에 들어갈 개별 항목들(예: 회식비 5만원, 교통비 3만원)을 나열합니다.

### 상태 전이 흐름
- `DRAFT`: 임시 저장 상태. 언제든 수정 가능합니다.
- `REQUESTED`: 승인자에게 결재가 올라간 상태입니다.
- `APPROVED`: 결재가 완료되었습니다. 이 시점에 전표 모듈(`journal-ledger`)로 회계 처리를 요청합니다.
- `REJECTED`: 결재가 반려된 상태입니다.

## 코드 분석 순서 (헥사고날 아키텍처 기준)

1. **Domain Model:** `ExpenditureResolution`, `ExpenditureDetail` (지출 상태 전이 및 도메인 규칙)
2. **Inbound Port:** `ExpenditureResolutionUseCase` (요청/승인/조회 등 유스케이스 정의)
3. **Application Service:** `ExpenditureResolutionService` (예산 차감, 결의 생성, 승인 시 전표 발행 조율)
4. **Outbound Port:** `ExpenditurePersistencePort` (DB 저장), `JournalLedgerPort` (전표 발행 API 연동)
5. **Inbound/Outbound Adapters:** REST 컨트롤러 및 JPA, FeignClient/WebClient 등.

## 변경 시 주의사항

- 지출결의서가 승인(`APPROVED`)될 때, `journal-ledger` 모듈의 전표 생성 API를 호출하는 로직(Outbound Adapter)이 성공해야 트랜잭션이 완료되는 구조를 고려해야 합니다. (혹은 이벤트 발행)
- 예산 차감 시점과 복원(반려 시) 시점의 트랜잭션 정합성이 중요합니다.
- 타 모듈 정보(거래처, 계정, 세금계산서)는 반드시 ID만 저장하고 Port/Adapter 구조를 통해 통신하여 멀티 스테이지 Docker 환경에서 서비스가 독립적으로 구동될 수 있도록 유지합니다.