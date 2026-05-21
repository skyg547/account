# reporting docs

`reporting` 모듈은 전표 데이터를 보고 라인으로 집계하고, 스냅샷을 저장하며, 보고 숫자에서 원천 전표까지 추적할 수 있게 한다.

읽기 순서:

1. [beginner-guide.md](/C:/Users/skyg547/IdeaProjects/account/reporting/docs/beginner-guide.md)
2. [process-flow.md](/C:/Users/skyg547/IdeaProjects/account/reporting/docs/process-flow.md)
3. [schema.md](/C:/Users/skyg547/IdeaProjects/account/reporting/docs/schema.md)

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
