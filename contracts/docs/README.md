# Contracts Module Docs

`contracts` 모듈은 MSA 간 의존을 직접 구현체에 묶지 않도록 포트와 커맨드/레퍼런스 DTO를 제공하는 계약 모듈입니다.

## 문서 목록

- [process-flow.md](./process-flow.md): 모듈 간 호출이 어떤 계약을 통해 연결되는지 설명합니다.
- [schema.md](./schema.md): 주요 포트, 커맨드, 레퍼런스 타입을 정리합니다.
- [beginner-guide.md](./beginner-guide.md): 초보자가 왜 이 모듈이 필요한지 쉽게 이해할 수 있도록 설명합니다.

## 이 모듈이 하는 일

1. 전표 생성 요청 규약을 정의합니다.
2. 마스터 조회 규약을 정의합니다.
3. 리스/자산/예산/세금/마감 연계 포트를 정의합니다.
4. 소스문서 드릴다운 공급자 규약을 정의합니다.

## 핵심 계약

- `JournalPostingPort`
- `MasterDataQueryPort`
- `SourceDocumentProvider`
- `BudgetControlPort`
- `LeasePaymentResolutionPort`
- `AssetRegistrationPort`
- `TaxInvoiceQueryPort`
- `AccountingPeriodStatusPort`

## 현재 구현 기준에서 먼저 알아둘 점

- 이 모듈에는 비즈니스 구현이 없습니다. 인터페이스와 레코드만 있습니다.
- 실제 동작은 각 업무 모듈이 이 계약을 구현하면서 완성됩니다.
- 다른 모듈 문서에서 보였던 연계는 대부분 여기 정의된 포트를 통해 연결됩니다.
  - `asset-lease` -> `LeasePaymentResolutionPort`
  - `expenditure-resolution` -> `BudgetControlPort`
  - `journal-ledger` 주변 -> `JournalPostingPort`, `SourceDocumentProvider`
  - `master-data` -> `MasterDataQueryPort`
