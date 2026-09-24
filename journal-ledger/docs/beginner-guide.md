# Journal Ledger 초보자 가이드

`journal-ledger`는 여러 업무 모듈에서 발생한 경제적 사건을 복식부기 전표로 기록하고, 승인·전기를 거쳐 GL/SL 원장과 미결 항목을 관리합니다.

## 먼저 이해할 용어

| 용어 | 쉬운 설명 | 시스템 의미 |
| --- | --- | --- |
| 전표 | 거래 한 건을 회계 언어로 번역한 기록 | `JournalEntry` 헤더와 여러 `JournalDetail` 라인 |
| 차변/대변 | 복식부기의 왼쪽/오른쪽 | `JournalSide.DEBIT`, `JournalSide.CREDIT` |
| 전기 | 승인된 전표를 실제 원장에 반영하는 작업 | GL/SL 생성과 잔액 갱신, `POSTED` 전환 |
| GL | 계정과목별 큰 장부 | `GlEntry`, `GlBalance` |
| SL | 거래처·부서 등 상세 보조 장부 | `SlEntry`, `SlBalance` |
| 미결 항목 | 아직 돈을 받거나 지급하지 않아 남아 있는 채권·채무 | `UnsettledItem` |
| 반제 | 미결 채권·채무를 수금·지급과 연결해 잔액을 줄이는 작업 | `OPEN -> PARTIAL -> CLEARED` |
| Lineage | 원천 문서까지 되짚는 추적 정보 | `lineageSourceType`, `lineageSourceId` |
| 거래통화 | 실제 거래 금액의 통화 | 전표 헤더 `currencyCode` |
| 기준통화 금액 | 거래 금액을 회사 장부 통화로 바꾼 금액 | 라인 `baseAmount`; 현재 기준통화는 KRW |

## 전표 한 건이 처리되는 순서

1. HTTP API는 거래 내용과 Gateway가 검증한 `X-Auth-User`/`X-Auth-Roles`를 받습니다. Kafka와 Spring contract 이벤트는 각각 리스너 전용 service principal을 maker로 사용합니다.
2. 수동 전표는 API DTO에서 도메인 객체로 변환되고, 자동 전표는 `JournalRuleEngine`이 규칙을 적용합니다.
3. 검증 엔진이 차대일치, 거래통화→KRW 환산, 기간 잠금, 계정 유효성을 확인합니다.
4. maker가 `DRAFT`를 `REQUESTED`로 제출하고, canonical identity가 다른 approver가 `APPROVED`로 전환합니다.
5. posting 역할을 가진 actor가 전기하면 GL/SL과 잔액을 갱신한 뒤 `POSTED`가 됩니다. 이때 최종 처리자와 별개로 `approvedBy`가 남습니다.
6. 채권·채무 라인은 필요 시 미결 항목으로 등록되고, 수금·지급 때 반제됩니다.

## 외화 전표를 이해하는 방법

전표 헤더의 거래통화와 환율은 한 세트입니다. 현재 기준통화는 KRW이며 환율은 “거래통화
1단위가 몇 KRW인가”를 뜻합니다. 그래서 USD 100과 환율 1300의 `baseAmount`는 100이 아니라
130,000입니다. 통화를 비우면 기존 입력과의 호환을 위해 KRW로 처리하고, KRW는 환율 생략 또는
1만 허용합니다. 외화는 양수 환율을 반드시 명시해야 합니다.

수동 API는 모든 통화에서 각 라인의 `baseAmount`를 직접 공급해야 합니다. Contract는 외화에서
반드시 공급해야 하지만, null/공백/KRW 통화와 환율 생략 또는 1인 동일단위 입력에는 누락값을
`amount`로 채우는 명시적인 호환 예외가 있습니다. 공급한 값이 공통 환산 규칙과 다르거나 외화
contract에서 빠지면 저장하지 않습니다. 자동분개는 이벤트의 명시적인 거래통화
(`transactionCurrencyCode`, `currencyCode`, `currency`)와 환율(`transactionToBaseRate`,
`exchangeRate`, `fxRate`)만 사용합니다. `functionalCurrency`, `reportingCurrency`나 회계정책의
기준통화를 거래통화/환율로 추측하지 않습니다. 여러 지원 별칭을 함께 보내면 정규화·숫자 비교
후 모두 같아야 하며, 같은 값의 중복은 허용하지만 충돌하면 전표를 만들지 않습니다.

여러 라인의 센트 반올림은 각 라인을 따로 반올림하지 않습니다. 차변·대변별 전체 목표 금액을
`HALF_UP`으로 정하고, 라인은 먼저 `DOWN`한 다음 소수 잔여가 큰 순서로 0.01을 배분합니다.
예를 들어 차변 0.01+0.02, 대변 0.03, 환율 1.33333333이면 차변 기준금액은 0.01+0.03,
대변은 0.04가 되어 양쪽이 정확히 0.04입니다.

일반 전표 환산은 작성 저장 전뿐 아니라 승인 요청, 승인, 전기 스냅샷에서도 다시 확인됩니다.
잘못된 기존 일반 초안이나 승인 전표는 자동 수정되지 않고 다음 단계에서 실패하므로 승인된
정정 절차가 필요합니다. 다만 관리형 역분개는 기존 `POSTED` 원본이 새 환산 의미에 맞지 않아도
원본의 환율과 거래/기준통화 금액을 그대로 복사하고 차대만 바꿔 실제 장부 효과를 취소합니다.
금액 정밀도와 양쪽 합계는 계속 검사하고, 공개·임의 역분개 생성은 차단합니다. 현재 계약은
환율 공급자·출처·적용일을 별도로 기록하거나 운영 데이터를 복구하지 않습니다.

## DDD/헥사고날 구조를 읽는 방법

```text
HTTP/Kafka Adapter
      -> Inbound Port
      -> Application Service
      -> Domain Method
      -> Outbound Port
      <- Persistence/External Adapter
```

- 인바운드 포트는 외부가 요청할 수 있는 유즈케이스를 정의합니다.
- 애플리케이션 서비스는 조회, 도메인 행위 호출, 저장 순서를 조정합니다.
- 도메인 객체는 차대변, 상태 전환, 반제 금액 같은 핵심 규칙을 지킵니다.
- 출력 포트는 저장소와 외부 시스템의 기술을 애플리케이션 계층에서 숨깁니다.
- 예: `UnsettledService -> UnsettledItemPersistencePort <- UnsettledItemPersistenceAdapter -> UnsettledItemRepository`.
- 자동분개 엔진도 `JournalRuleQueryPort`만 사용하고, 세부 Spring Data 저장소 조회는 `JournalRuleQueryAdapter`가 담당합니다.
- 전기 서비스는 `LedgerEntryPersistencePort`, 잔액 서비스는 `LedgerBalancePersistencePort`를 사용하므로 애플리케이션 서비스가 JPA Repository를 직접 알지 않습니다.
- 기본 저장은 JPA 어댑터가 맡고, 운영 대용량 경로는 `journal-ledger.ledger.persistence-mode=jdbc-bulk` 설정으로 JDBC batch/upsert 어댑터를 사용할 수 있습니다.
- 계층별 책임과 레거시 README 아카이브는 [layer-guide.md](layer-guide.md)를 기준으로 확인합니다.

## 미결 반제를 읽는 방법

1. `UnsettledController`가 미결 ID, 금액, `settlementReference`, `X-User-ID`를 받습니다.
2. `UnsettledItemUseCase`를 통해 `UnsettledService`를 호출합니다.
3. 서비스는 출력 포트로 미결 항목을 조회합니다.
4. `UnsettledItem.settle()`이 금액 검증, 중복 참조 확인, 상태 전환, 감사 정보를 처리합니다.
5. 서비스가 변경된 도메인 객체를 출력 포트로 저장합니다.

동일한 `settlementReference`가 재전송되면 금액을 다시 차감하지 않습니다. 거래처별 미결 조회는 전체 데이터를 메모리에서 거르지 않고 DB 조건으로 조회합니다.

## 개발자가 먼저 확인할 질문

- 전표가 차변/대변 균형을 만족하는가?
- 거래통화·환율·각 라인의 기준통화 금액이 같은 KRW 환산 정책을 따르는가?
- 실제 처리자와 원천 문서 추적 키가 전달되는가?
- maker와 approver가 trim/lowercase 후에도 서로 다른 canonical identity인가?
- 원장 조회는 `POSTED` 전표만 포함하는가?
- 같은 외부 요청이 재시도되어도 중복 전기·반제가 발생하지 않는가?
- 애플리케이션 서비스가 JPA 저장소를 직접 참조하지 않는가?

재집계는 "새 장부가 완성될 때까지 공사 중 표지판을 세우는 작업"입니다. 시작하면 영속
제어 행이 닫혀 API/Monolith 잔액 조회와 전기가 실패하고, 장애가 나도 자동으로 열리지 않습니다.
운영자는 같은 JobParameters의 같은 JobInstance를 재시작해야 하며, 마지막 GL/SL 대사가
성공한 뒤에만 조회와 전기가 다시 열립니다.

상세 호출 흐름은 [process-flow.md](process-flow.md), 테이블 관계는 [schema.md](schema.md)를 참고합니다.

## 로컬 실행

저장소 루트에서 외화 환산을 포함한 모듈 회귀를 실행합니다. 이 문서는 실행 결과를 선언하지
않으며 실제 결과는 Issue/PR 검증 기록을 확인합니다.

```bash
./gradlew :journal-ledger:test
```

```powershell
.\gradlew :journal-ledger:core:test :journal-ledger:api:test --console=plain --max-workers=1 --no-daemon
.\gradlew :journal-ledger:api:bootRun --console=plain
```

JDBC bulk 모드 확인:

```powershell
.\gradlew :journal-ledger:api:bootRun --args="--journal-ledger.ledger.persistence-mode=jdbc-bulk" --console=plain
```

IntelliJ에서는 `Journal Ledger API bootRun`, `Journal Ledger API JDBC Bulk` 공유 실행 설정을 사용합니다.
