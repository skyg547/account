# reporting docs

`reporting` 모듈은 전표 데이터를 보고 라인으로 집계하고, 스냅샷을 저장하며, 보고 숫자에서 원천 전표까지 추적할 수 있게 합니다.

## 문서 목록

- [beginner-guide.md](./beginner-guide.md)
- [process-flow.md](./process-flow.md)
- [schema.md](./schema.md)
- [local-run.md](./local-run.md)

## 현재 실행 전제

- `reporting`은 상위 집계 프로젝트이고 실제 코드는 `reporting:core`, `reporting:api`, `reporting:batch`에 있다.
- `reporting:core`는 현재 `java-library` 모듈이다.
- `reporting:api`와 `reporting:batch`는 standalone Spring Boot 앱이며 각각 `:reporting:api:bootRun`, `:reporting:batch:bootRun`으로 실행한다.
- `reporting:api`는 HTTP 응답 DTO를 소유한다. Controller는 core command를 호출하고, core 도메인 결과를 response DTO로 변환해 외부 JSON 계약을 고정한다.
- 로컬 학습 실행은 `account.reporting.persistence.mode=memory`를 사용해 journal-ledger 없이 샘플 GL 잔액으로 기동한다.
- 검증은 IntelliJ Gradle 실행 구성 또는 `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test`로 수행한다.

## 주요 특징 및 구현 기준 (Phase 4 반영)

- **외부 규제기관 연동 아키텍처 (Gateway Adapter)**:
  - `LocalRegulatoryFilingGatewayAdapter`를 통해 감독기관 전송 프로토콜을 캡슐화합니다.
  - 가상의 MTLS 인증 흐름과 설정 기반 실패 시뮬레이션을 제공합니다.
  - 실패 시뮬레이션은 `account.reporting.regulatory-filing.failure-simulation.enabled=true`일 때만 항상 같은 메시지로 동작하므로 테스트 재현성을 해치지 않습니다.
  - 생성된 접수증(Receipt)은 로컬의 단순 시퀀스가 아닌 UUID 포맷을 포함한 규제 표준 형태로 반환됩니다.
- **Fail-Closed 데이터 생성 제어**:
  - 주석 마트(Disclosure Note Mart) 및 감독보고 매핑 데이터 추출 시 필드가 불일치하거나 누락된 항목은 강제로 0으로 대체하지 않고 명시적인 실패/거절을 유발하여 보고 데이터의 무결성을 유지합니다.


## API Response DTO Boundary

- `ReportingController`는 요청 파라미터와 `X-User-ID`를 core command로 변환한다.
- `FinancialStatementResponseDto`, `DisclosureNoteMartResponseDto`, `RegulatoryReportSubmissionResponseDto`, `RegulatoryFilingResponseDto`가 외부 JSON 응답을 담당한다.
- `JournalDetailSummaryResponseDto`는 drill-down 응답에서 journal-ledger contract 객체가 HTTP 응답으로 직접 새는 것을 막는다.
- 초보자 관점에서는 core 도메인 객체는 "업무 상태와 규칙", API DTO는 "화면/외부 시스템에 보여줄 모양"으로 구분하면 된다.
## Document Export API

- Endpoint: `POST /api/v1/reporting/generate/document`
- Required query params: `type`, `baseDate`
- Optional query param: `format` (`PDF` or `EXCEL`, default `PDF`)
- Required header: `X-User-ID`

Example request:

```http
POST /api/v1/reporting/generate/document?type=BALANCE_SHEET&baseDate=2026-03-31T00:00:00&format=EXCEL
X-User-ID: tester
```

Response:
- Binary file download
- `Content-Type`: `application/pdf` (PDF) or `text/csv; charset=UTF-8` (EXCEL)
- `Content-Disposition`: attachment filename

## Regulatory Submission API

- Endpoint: `POST /api/v1/reporting/submissions/regulatory`
- Required query params: `type`, `baseDate`
- Optional query param: `correctionReason` (required from version 2)
- Required header: `X-User-ID`

Example request:

```http
POST /api/v1/reporting/submissions/regulatory?type=BALANCE_SHEET&baseDate=2026-03-31T00:00:00
X-User-ID: tester
```

Response:
- `submissionId`, `statementId`, `statementType`, `baseDate`, `version`, `submittedBy`, `submittedAt`, `correctionReason`, `status`

## Disclosure Note Mart API

- Generate endpoint: `POST /api/v1/reporting/disclosure-notes/generate`
- Query endpoint: `GET /api/v1/reporting/disclosure-notes`
- Required query params: `type`, `baseDate`
- Generate required header: `X-User-ID`

Example generate request:

```http
POST /api/v1/reporting/disclosure-notes/generate?type=BALANCE_SHEET&baseDate=2026-03-31T00:00:00
X-User-ID: tester
```

Response:
- `martId`, `statementId`, `statementType`, `baseDate`, `generatedBy`, `generatedAt`
- `entries[]`: `noteNumber`, `noteCategory`, `sourceLineCode`, `maturityBucket`, `rateType`, `currencyCode`, `riskCategory`, `currentAmount`, `previousAmount`

## Regulatory Filing API

- Submit endpoint: `POST /api/v1/reporting/regulatory-filings/submit`
- Latest query endpoint: `GET /api/v1/reporting/regulatory-filings/latest`
- Required query params: `type`, `baseDate`
- Optional query param: `targetAgency` (`FSS` default)
- Submit required header: `X-User-ID`

Example submit request:

```http
POST /api/v1/reporting/regulatory-filings/submit?type=BALANCE_SHEET&baseDate=2026-03-31T00:00:00&targetAgency=FSS
X-User-ID: tester
```

Response:
- `filingId`, `submissionId`, `targetAgency`, `status`, `regulatorReceiptId`, `regulatorMessage`
- `lines[]`: `reportCode`, `fieldCode`, `fieldLabel`, `sourceNoteNumber`, `sourceLineCode`, `currentAmount`, `previousAmount`

## Batch Adapter

- `reporting:batch`에는 `ReportingBatchAdapter`가 있다.
- `ReportingStatementBatchConfig`가 Spring Batch `reportingStatementGenerationJob`과 `reportingStatementGenerationStep`을 제공한다.
- JobParameter는 `baseDate=yyyy-MM-dd`, `requester=사용자ID` 두 개가 필수다.
- 배치 모듈은 파라미터 검증과 실행 흐름만 담당하고, 재무제표 생성 규칙은 core의 `GenerateStatementUseCase`가 처리한다.
- 기본 `spring.batch.job.enabled=false`라서 단순 `bootRun`은 컨텍스트 기동 검증용으로만 실행된다.
- 실제 배치 실행 예:

```powershell
.\gradlew :reporting:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=reportingStatementGenerationJob baseDate=2026-03-31 requester=local-batch"
```
