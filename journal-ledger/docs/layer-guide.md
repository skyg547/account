# Journal Ledger 계층 가이드

이 문서는 예전 소스 트리 하위 README의 초보자 설명을 살리되, 현재 코드 구조에 맞게 정리한 계층 안내서입니다.

## 아카이브한 레거시 문서

아래 문서는 삭제하지 않고 `journal-ledger/docs/archive`에 보존했습니다.

- [archive/application-layer-readme-legacy.md](archive/application-layer-readme-legacy.md)
- [archive/domain-layer-readme-legacy.md](archive/domain-layer-readme-legacy.md)
- [archive/adapter-layer-readme-legacy.md](archive/adapter-layer-readme-legacy.md)

레거시 문서의 설명은 학습에는 유용하지만, 현재 코드에서는 JPA 엔티티가 `domain` 패키지에 남아 있는 등 일부 규칙이 실제와 다릅니다. 새 개발자는 이 문서를 기준으로 보고, 과거 표현이 필요한 경우 archive를 참고합니다.

## 전체 의존 방향

```text
adapter/in/web or adapter/in/kafka
        -> application/port/in
        -> application/service
        -> domain
        -> application/port/out
        <- infrastructure/persistence or adapter/out
```

## application 계층

application 계층은 유즈케이스 흐름을 조정합니다.

- 입력 포트: 외부가 호출할 수 있는 기능 계약입니다.
- 출력 포트: 저장소, 외부 API, 메시징 같은 기술을 숨기는 계약입니다.
- service: 트랜잭션 경계, 조회 순서, 도메인 메서드 호출, 저장 순서를 조정합니다.

예시:

- `PostingService`는 승인 전표를 GL/SL 엔트리로 변환하고 `LedgerEntryPersistencePort`로 저장합니다.
- `LedgerService`는 잔액 집계 흐름을 담당하고 `LedgerBalancePersistencePort`에 저장/조회 세부사항을 위임합니다.
- `UnsettledService`는 미결 반제 유즈케이스를 조정하고 `UnsettledItem` 도메인 메서드가 상태 전이를 처리하게 합니다.

## domain 계층

domain 계층은 전표, 전표 라인, 자동분개 규칙, 원장, 미결 항목의 핵심 업무 의미를 담습니다.

현재 저장소에서는 일부 JPA 엔티티가 domain 패키지에 위치합니다. 이상적인 순수 도메인과 완전히 같지는 않으므로, 새 기능을 넣을 때는 아래 기준을 따릅니다.

- 상태 전이와 금액 검증은 도메인 메서드에 둡니다.
- 외부 저장소 조회, HTTP 호출, 캐시, JDBC SQL은 domain에 두지 않습니다.
- 차대변 문자열은 `JournalSide`로 제한하고, 실제 금액 방향은 불변 `Debit`/`Credit` VO로 표현합니다.
- `AccountingPrecision`은 원장 금액 `DECIMAL(19,2)`와 환율 `DECIMAL(19,8)`을 강제하며 무음 반올림을 금지합니다.
- `JournalEntry`는 상세 라인의 소유권과 거래/기준통화 차대일치를, `GeneralLedger`는 승인된 전표의 불변 posting snapshot을 책임집니다.

## Batch 계층

`journal-ledger:batch`는 inbound adapter입니다. Job/Step과 Tasklet은 Spring Batch 기술 객체를 다루지만, 전표·원장 업무 규칙은 core application service에 둡니다.

- `BalanceReaggregationBatchConfig`: Job/Step wiring만 담당합니다.
- `BalanceReaggregationTasklet`: JobParameter를 기간으로 변환하고 `LedgerService`를 호출합니다.
- `BatchDateRangeParameterUtils`: Spring Batch `JobParameters`를 `LocalDate` 범위로 바꿉니다.
## adapter/in 계층

adapter/in은 외부 요청을 내부 유즈케이스 호출로 바꾸는 계층입니다.

- Web Controller는 DTO 검증, 헤더 추출, 응답 변환을 담당합니다.
- Kafka Listener는 이벤트 역직렬화와 실패 전파를 담당합니다.
- Controller가 JPA Repository를 직접 호출하지 않습니다.

## infrastructure/persistence와 adapter/out 계층

저장 기술은 출력 포트 뒤에 둡니다.

| 기능 | 출력 포트 | 기본 어댑터 | 대량 어댑터 |
| --- | --- | --- | --- |
| 전표 저장 | `JournalPersistencePort` | JPA | 별도 필요 시 추가 |
| 자동분개 규칙 조회 | `JournalRuleQueryPort` | JPA | - |
| GL/SL 엔트리 저장 | `LedgerEntryPersistencePort.save(GeneralLedger)` | Aggregate → JPA entity mapping 후 `saveAll` | 같은 Aggregate를 JDBC batch insert |
| GL/SL 잔액 저장 | `LedgerBalancePersistencePort` | JPA `saveAll` | JDBC upsert/update+insert |
| 미결 항목 저장 | `UnsettledItemPersistencePort` | JPA | - |

잔액 변경은 `lockBalanceAccounts`로 이번 호출의 모든 계정·통화 잠금을 먼저 획득한 뒤
조회/계산/저장합니다. 기간 삭제·단일 트랜잭션 재집계는 `lockAllBalanceAccounts`를 사용합니다.
`LedgerBalanceWriteLock`은 256개 DB 잠금 행의 정렬, 트랜잭션/isolation 검증과 최신 값 refresh를
두 어댑터에 공유합니다. 금액 계산과 이월 규칙은 계속 `LedgerService`와 도메인에 남습니다.
포트의 `save*`는 절대 금액 저장이므로 호출자가 같은 트랜잭션에서 잠금을 선점해야 하며,
저장 메서드만 직접 호출하는 경로에 동시성 보장을 기대하면 안 됩니다.

대량 전기 모드는 아래 설정으로 켭니다.

```yaml
journal-ledger:
  ledger:
    persistence-mode: jdbc-bulk
```

## 새 코드를 추가할 때 체크

- API만 바꾸는가, 도메인 규칙도 바꾸는가를 먼저 분리합니다.
- 전표 금액은 차대일치 검증을 반드시 거칩니다.
- 전기 결과 조회는 `POSTED` 상태만 재무 금액으로 취급합니다.
- 미결 반제는 `settlementReference`로 중복 반영을 막습니다.
- 대량 저장은 service loop가 아니라 adapter의 batch/upsert 기능으로 해결합니다.
