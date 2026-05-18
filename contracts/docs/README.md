# Contracts Module Docs

`contracts` 모듈은 MSA 간 의존성을 물리적인 구현체에 묶지 않기 위해 포트(Port)와 DTO(Command/Ref) 규약을 정의하는 통신 양식 저장소입니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**Q. 이 모듈은 왜 필요한가요?**
마이크로서비스 구조에서 `asset-lease`가 `expenditure-resolution`의 내부 클래스를 직접 호출하면 의존성이 꼬이고 스파게티 코드가 됩니다.
이를 막기 위해, 구현 코드는 각자 모듈에 두고 **"서로 대화할 때 쓸 A4 결재 서류 양식(인터페이스, DTO)"**만 이곳 `contracts`에 모아둡니다.

**초보자가 알아야 할 3가지 핵심 요소:**
1. **Port (포트):** 다른 부서에 요청을 던지는 창구 (예: `JournalPostingPort`).
2. **Command (커맨드):** 요청할 때 빈칸을 채워서 보내는 양식 (예: `JournalEntryCommand`).
3. **Ref (레퍼런스):** 복잡한 마스터 데이터 대신 이름표만 가져오는 DTO (예: `AccountSubjectRef`).

> ⚠️ **주의:** 이 모듈에는 비즈니스 구현 로직이 없습니다. 껍데기만 존재하며, 단독 실행되는 서버가 아니므로 `docker-compose` 구동 대상이 아닙니다. 실제 동작은 각 업무 모듈(Adapter)이 이 포트를 구현하면서 완성됩니다.

---

## 2. 🔄 처리 흐름 (Process Flow)

호출 모듈은 `contracts`의 포트만 알고 있으며, 실제 수행은 구현 모듈이 담당합니다.

### 전표 생성 흐름 (Journal)
1. **호출 모듈** -> `createDraftEntry(JournalEntryCommand)` -> `JournalPostingPort`
2. **구현 모듈 (`journal-ledger`)** 어댑터가 이를 받아 실제 전표 엔티티를 생성하고 원장에 반영.
3. 결과로 `JournalPostingResult` 반환.

### 전표 조회/집계 흐름 (Journal Query)
1. **호출 모듈** -> `getJournalDetailAggregate(startDate, endDate, side)` -> `JournalQueryPort`
2. **구현 모듈 (`journal-ledger`)** 어댑터가 DB 집계 쿼리로 기간/차대변 기준 상세 건수와 금액 합계를 반환.
3. 결과로 `JournalDetailAggregateSummary`를 받아 대량 전표 상세를 애플리케이션 메모리에서 순회하지 않아도 됩니다.

### 원장 잔액 조회 흐름 (Ledger Query)
1. **호출 모듈** -> `getGlBalanceSummaries(...)` 또는 `getSlBalanceSummaries(...)` -> `LedgerQueryPort`
2. **구현 모듈 (`journal-ledger`)** 어댑터가 GL/SL 잔액을 계정, 통화, 거래처, 부서 차원으로 조회합니다.
3. 결과로 `LedgerBalanceSummary`를 받아 보고/대사 모듈이 journal-ledger 내부 엔티티를 직접 참조하지 않고 잔액을 사용할 수 있습니다.

### 마스터 데이터 조회 흐름 (Master Data)
1. **호출 모듈** -> `findAccountSubject("10100")` -> `MasterDataQueryPort`
2. **구현 모듈 (`master-data`)** 어댑터가 DB 조회 후 `AccountSubjectRef` (이름표 DTO) 반환.
- 이를 통해 타 모듈이 `master-data`의 엔티티를 직접 바라보지 않아도 됩니다.

### 예산/지출/대사 흐름
- **리스 지급:** `asset-lease` -> `LeasePaymentResolutionPort` -> `expenditure-resolution` (지출 결의서 생성)
- **드릴다운:** 각 모듈이 `SourceDocumentProvider`를 구현하여, 전표에서 원문서를 추적(`lineageSourceType` 기반).

---

## 3. 💾 주요 계약 스키마 (Schema)

이 모듈은 DB 테이블이 없으며, 아래와 같은 데이터 전송 객체(DTO)와 포트 명세로 구성됩니다.

- **Journal (전표/원장):** `JournalPostingPort`, `JournalQueryPort`, `LedgerQueryPort`, `JournalEntryCommand`, `JournalLineCommand`, `JournalPostingResult`, `JournalDetailAggregateSummary`, `LedgerBalanceSummary`
- **Master Data (기준정보):** `MasterDataQueryPort`, `FiscalPeriodControlPort`, `AccountSubjectRef`, `BusinessPartnerRef`, `DepartmentRef`, `FiscalPeriodRef`
- **Source (추적):** `SourceDocumentProvider` (전표에서 원문서 역추적)
- **Expenditure (지출):** `BudgetControlPort`, `LeasePaymentResolutionPort`, `LeasePaymentResolutionCommand`
- **Asset (자산):** `AssetRegistrationPort`, `AssetAcquisitionCommand`
- **Tax & Closing (세금/마감):** `TaxInvoiceQueryPort`, `AccountingPeriodStatusPort`
