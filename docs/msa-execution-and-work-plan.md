# MSA 실행 및 작업 순서 계획

이 문서는 `account` 프로젝트를 MSA 구조로 전환할 때 어떤 모듈을 먼저 실행하고, 어떤 모듈부터 리팩토링해야 하는지 정리한다.

## 1. 기본 원칙

- 실행 순서와 개발 순서는 다르다.
- 실행 순서는 인프라 의존성 기준으로 정한다.
- 개발 순서는 도메인 의존성 기준으로 정한다.
- 모든 업무 모듈은 가능하면 다른 모듈의 JPA 엔티티나 Repository를 직접 참조하지 않는다.
- 모듈 간 호출은 `contracts`의 Port, DTO, Event 계약을 우선 사용한다.
- 로컬에서는 모든 서비스를 항상 띄우지 않고, 현재 작업에 필요한 최소 조합만 띄운다.

## 2. 전체 실행 순서

전체 MSA 구성을 한 번에 올릴 때는 다음 순서를 권장한다.

1. `discovery`
2. `config-server`
3. `auth`
4. `governance`
5. `master-data`
6. `journal-ledger:api`
7. `receivable`
8. `payable`
9. `asset-lease`
10. `loan`
11. `tax`
12. `closing`
13. `reconciliation`
14. `reporting`
15. `expenditure-resolution`
16. `gateway`

`gateway`는 외부 진입점이므로 마지막에 띄운다. `discovery`, `config-server`는 다른 서비스의 기반이므로 먼저 띄운다.

## 3. 기본 실행 명령

Windows PowerShell 기준으로 JDK 21을 명시해서 실행한다.

```powershell
$env:JAVA_HOME='C:\Java\jdk21'
$env:Path='C:\Java\jdk21\bin;' + $env:Path
```

서비스별 실행 명령은 다음과 같다.

```powershell
.\gradlew.bat :discovery:bootRun
.\gradlew.bat :config-server:bootRun
.\gradlew.bat :auth:bootRun
.\gradlew.bat :governance:bootRun
.\gradlew.bat :master-data:bootRun
.\gradlew.bat :journal-ledger:api:bootRun
.\gradlew.bat :receivable:bootRun
.\gradlew.bat :payable:bootRun
.\gradlew.bat :gateway:bootRun
```

각 서비스는 별도 터미널에서 실행한다.

## 4. 작업 우선순위

MSA 전환 작업은 다음 순서로 진행한다.

1. `contracts`
2. `master-data`
3. `journal-ledger`
4. `receivable`
5. `payable`
6. `gateway`
7. `auth`
8. `governance`
9. `asset-lease`
10. `loan`
11. `tax`
12. `closing`
13. `reconciliation`
14. `reporting`
15. `expenditure-resolution`

처음 작업은 `contracts -> master-data` 순서가 가장 안전하다. 기준정보 경계가 흔들리면 모든 업무 모듈의 의존성이 같이 흔들리기 때문이다.

## 5. 1단계 작업: contracts 정리

목표는 모듈 간 직접 참조를 끊을 수 있는 최소 계약을 만드는 것이다.

- `MasterDataQueryPort`를 기준정보 조회 계약으로 확정한다.
- `JournalPostingPort`를 전표 생성 계약으로 확정한다.
- 거래처, 계정과목, 부서, 통화는 Entity 대신 Ref DTO로 주고받는다.
- 업무 모듈에서 `com.ho.account.basic.domain.*` 직접 참조를 줄인다.
- 업무 모듈에서 `JournalService`, `JournalEntry`, `JournalDetail` 직접 참조를 줄인다.

완료 기준은 다음과 같다.

- `contracts`가 특정 구현 모듈에 의존하지 않는다.
- 업무 모듈이 다른 모듈 Repository를 직접 주입받지 않는다.
- `:contracts:compileJava`가 성공한다.

## 6. 2단계 작업: master-data 분리

목표는 기준정보의 소유권을 `master-data`로 고정하는 것이다.

- `api`, `core`, `batch` 구조를 명확히 분리한다.
- `AccountSubject`, `BusinessPartner`, `Department`, `Currency`의 외부 노출은 contracts DTO로 제한한다.
- 외부 모듈이 기준정보를 직접 저장하거나 수정하지 않도록 한다.
- `MonolithMasterDataQueryAdapter`는 점진 전환용으로 유지하되, 최종적으로는 REST/Feign 어댑터로 교체한다.

완료 기준은 다음과 같다.

- 기준정보 조회는 `MasterDataQueryPort`를 통한다.
- master-data 단독 실행이 가능하다.
- `:master-data:test` 또는 최소 `:master-data:compileJava`가 성공한다.

## 7. 3단계 작업: journal-ledger 분리

목표는 전표와 원장의 소유권을 `journal-ledger`로 고정하는 것이다.

- 외부 모듈은 전표 엔티티를 직접 생성하지 않는다.
- 외부 모듈은 `JournalPostingPort`로 전표 생성을 요청한다.
- `journal-ledger:core`에는 도메인과 유스케이스를 둔다.
- `journal-ledger:api`에는 REST Controller와 외부 어댑터를 둔다.
- `journal-ledger:batch`에는 재집계, 마감성 배치 작업을 둔다.

완료 기준은 다음과 같다.

- `receivable`, `payable`이 `JournalService`를 직접 참조하지 않는다.
- `journal-ledger:api` 단독 실행이 가능하다.
- `:journal-ledger:core:compileJava`, `:journal-ledger:api:compileJava`가 성공한다.

## 8. 4단계 작업: receivable/payable 분리

목표는 매출채권/매입채무 업무 모듈을 독립 서비스로 실행 가능하게 만드는 것이다.

- 거래처는 `BusinessPartner` Entity 대신 거래처 코드와 Ref DTO 중심으로 바꾼다.
- 계정과목 검증은 `MasterDataQueryPort`를 사용한다.
- 전표 생성은 `JournalPostingPort`를 사용한다.
- 자체 DB 테이블과 Flyway 마이그레이션을 정리한다.

완료 기준은 다음과 같다.

- `:receivable:compileJava`, `:payable:compileJava`가 성공한다.
- 각 모듈이 독립 `bootRun` 가능한 상태가 된다.
- gateway 라우팅을 통해 API 호출이 가능하다.

## 9. 최소 실행 조합

항상 전체 서비스를 띄우지 않는다. 작업 대상에 따라 최소 조합만 실행한다.

### master-data 작업

```powershell
.\gradlew.bat :discovery:bootRun
.\gradlew.bat :config-server:bootRun
.\gradlew.bat :master-data:bootRun
.\gradlew.bat :gateway:bootRun
```

### journal-ledger 작업

```powershell
.\gradlew.bat :discovery:bootRun
.\gradlew.bat :config-server:bootRun
.\gradlew.bat :master-data:bootRun
.\gradlew.bat :journal-ledger:api:bootRun
.\gradlew.bat :gateway:bootRun
```

### receivable 작업

```powershell
.\gradlew.bat :discovery:bootRun
.\gradlew.bat :config-server:bootRun
.\gradlew.bat :master-data:bootRun
.\gradlew.bat :journal-ledger:api:bootRun
.\gradlew.bat :receivable:bootRun
.\gradlew.bat :gateway:bootRun
```

### payable 작업

```powershell
.\gradlew.bat :discovery:bootRun
.\gradlew.bat :config-server:bootRun
.\gradlew.bat :master-data:bootRun
.\gradlew.bat :journal-ledger:api:bootRun
.\gradlew.bat :payable:bootRun
.\gradlew.bat :gateway:bootRun
```

## 10. 로컬 리소스 기준

서비스 하나당 Spring Boot JVM은 대략 400MB에서 800MB 정도를 예상한다.

권장 사양은 다음과 같다.

- 최소 개발: 4코어, RAM 16GB
- 안정 개발: 8코어, RAM 32GB
- 전체 인프라 포함: 8코어 이상, RAM 32GB 이상

Kafka, Redis, Vault, Elasticsearch, Logstash, Kibana, Prometheus, Grafana, Zipkin을 모두 함께 올리면 16GB 환경은 부족할 가능성이 높다.

## 11. 당장 다음 작업

바로 다음 작업은 `contracts -> master-data` 경계 정리다.

1. `contracts`의 master-data/journal 계약을 점검한다.
2. `master-data`가 contracts 구현체를 안정적으로 제공하는지 확인한다.
3. `receivable`, `payable`에 남은 직접 엔티티/Repository 참조를 줄인다.
4. 각 단계마다 compile/test를 통과시킨다.
5. 단계별로 작은 커밋을 만든다.
