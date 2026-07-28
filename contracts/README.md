# Contracts 모듈: 모듈 간 결재 양식과 연결 규칙

## 왕초보를 위한 개념

`contracts`는 서로 다른 업무 부서가 협업할 때 사용하는 **공용 결재 양식**입니다.

예를 들어 대출 업무가 회계 원장에 전표 생성을 요청할 때 journal-ledger 내부 엔티티를 직접
가져오지 않습니다. 대신 `JournalEntryCommand`라는 양식을 채워 `JournalPostingPort`에
전달합니다. 이 방식은 상대 모듈의 DB/JPA 구조가 바뀌어도 호출 모듈이 함께 무너지는 것을 막습니다.

다만 한 가지를 구분해야 합니다.

- `contracts`의 Java 인터페이스는 그 자체로 REST나 Kafka를 호출하지 않습니다.
- 같은 Spring 프로세스에서는 구현 Bean을 주입받아 JVM 메서드로 호출합니다.
- 서비스를 별도 컨테이너로 분리하면 Feign/REST/Kafka 어댑터가 같은 계약 의미를 구현해야 합니다.
- 따라서 이 모듈은 네트워크 서버도, 실행 가능한 Spring Boot 애플리케이션도 아닙니다.

## 헥사고날 관점

```text
호출 Application Service
    -> contracts Port/Command
        -> 제공 모듈 Adapter
            -> 제공 모듈 Application/Domain
                -> DB 또는 외부 시스템
```

호출 모듈에서는 포트를 아웃바운드 포트로 사용합니다. 제공 모듈에서는 어댑터가 그 계약을
구현합니다. 계약에는 JPA 엔티티, Controller, Repository 같은 기술 타입을 넣지 않습니다.

현재 주요 계약:

- 전표: `JournalPostingPort`, `JournalQueryPort`, `JournalEntryCommand`
- 원장: `LedgerQueryPort`, `LedgerBalanceSummary`
- 기준정보: `MasterDataQueryPort`, `AccountSubjectRef`, `FiscalPeriodControlPort`
- 지출/리스: `BudgetControlPort`, `LeasePaymentResolutionPort`
- 자산: `AssetRegistrationPort`
- 세금/마감: `TaxInvoiceQueryPort`, `AccountingPeriodStatusPort`
- 원문서 추적: `SourceDocumentProvider`

## 이번 점검에서 강화한 정합성

- 전표 일자는 필수이며 라인 목록은 비어 있을 수 없습니다.
- 전표 라인은 `DEBIT/CREDIT`, 계정 코드, 금액을 생성 시점에 검증합니다.
- 라인 목록을 불변 복사하여 호출자가 원본 List를 바꿔 전표 내용이 변하지 않게 했습니다.
- 계정과목 정상잔액 방향은 `DEBIT/CREDIT` 외 값을 거부합니다.
- master-data의 실제 어댑터가 기준일 SCD2 조회를 수행하도록 연결했습니다.

## 남은 구조 개선 TODO

- 모든 Master Data 제공자가 기준일 조회를 구현하면 현재 호환용 default 메서드를 제거합니다.
- `SourceDocumentProvider`의 `Map<String, Object>`를 버전이 있는 DTO 계약으로 교체합니다.
- shared-kernel 타입을 public API에서 노출하는 로컬 capability SPI를 최소 계약 모듈로 분리합니다.
- 기존 mutable 조회 DTO는 소비처/직렬화 호환성을 확인한 뒤 record/불변 projection으로 전환합니다.

## 문서와 검증

문서는 [docs/README.md](./docs/README.md)에서 다음 순서로 읽습니다.

1. `beginner-guide.md`
2. `process-flow.md`
3. `schema.md`
4. `local-run.md`

```powershell
.gradlew :contracts:test :contracts:compileJava --console=plain --max-workers=1 --no-daemon
```

IntelliJ에서는 `Foundation Library Compile`을 사용합니다. H2/PostgreSQL이나 내장 WAS는 필요하지
않습니다. 과거 빈 Dockerfile/Compose는 삭제하지 않고
[docs/archive/legacy-runtime-skeleton](./docs/archive/legacy-runtime-skeleton/README.md)에 보존했습니다.