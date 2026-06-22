# deposit docs

`deposit` 문서는 예금 계좌 개설과 초기입금 전표 생성 흐름을 설명합니다.

## 문서 목록

- [local-run.md](./local-run.md)

## 현재 실행 전제

- `deposit:core`는 도메인/애플리케이션/어댑터를 담는 library 모듈입니다.
- `deposit:api`는 `DepositApplication`을 main class로 갖는 Spring Boot API 앱입니다.
- `deposit:batch`는 `DepositBatchApplication`을 main class로 갖는 Spring Boot Batch 앱이며, `depositAccountIntegrityJob`을 실행할 수 있습니다.
- 로컬 단독 실행은 `account.deposit.local-adapters.enabled=true`를 켜서 master-data/journal-ledger 외부 포트를 학습용 어댑터로 대체합니다.

## 운영 연결 시 주의사항

- 계정과목 검증은 `MasterDataQueryPort`의 실제 구현으로 교체해야 합니다.
- 초기입금 전표 생성은 `JournalPostingPort`의 실제 journal-ledger 어댑터로 교체해야 합니다.
- 로컬 어댑터는 운영 회계 전표를 저장하지 않고 식별자만 반환합니다.
