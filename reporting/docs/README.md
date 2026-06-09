# reporting docs

`reporting` 모듈은 전표 데이터를 보고 라인으로 집계하고, 스냅샷을 저장하며, 보고 숫자에서 원천 전표까지 추적할 수 있게 합니다.

## 문서 목록

- [beginner-guide.md](./beginner-guide.md)
- [process-flow.md](./process-flow.md)
- [schema.md](./schema.md)

## 주요 특징 및 구현 기준 (Phase 4 반영)

- **외부 규제기관 연동 아키텍처 (Gateway Adapter)**:
  - `LocalRegulatoryFilingGatewayAdapter`를 통해 감독기관 전송 프로토콜을 캡슐화합니다.
  - 가상의 MTLS 인증 흐름, 5% 확률의 랜덤 실패 시뮬레이션을 내장하여, 실제 운영 환경과 유사한 네트워크 불안정성과 반려 응답 처리를 테스트할 수 있게 합니다.
  - 생성된 접수증(Receipt)은 로컬의 단순 시퀀스가 아닌 UUID 포맷을 포함한 규제 표준 형태로 반환됩니다.
- **Fail-Closed 데이터 생성 제어**:
  - 주석 마트(Disclosure Note Mart) 및 감독보고 매핑 데이터 추출 시 필드가 불일치하거나 누락된 항목은 강제로 0으로 대체하지 않고 명시적인 실패/거절을 유발하여 보고 데이터의 무결성을 유지합니다.

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
