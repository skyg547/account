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

## 2.1 실제 운영 적용 순서

운영 적용 순서는 "먼저 띄우는 순서"가 아니라 "장애 영향과 데이터 정합성 위험이 낮은 것부터 실제 업무 트래픽을 받게 하는 순서"다.

권장 순서는 다음과 같다.

| 단계 | 운영 적용 대상 | 운영 방식 | 선행 조건 |
| --- | --- | --- | --- |
| 0 | `shared-kernel`, `contracts` | 라이브러리/계약 배포 | `:contracts:compileJava` 성공, 하위 호환 생성자 유지 |
| 1 | `discovery`, `config-server`, `auth`, `gateway` | 인프라 먼저 무업무 트래픽으로 검증 | 서비스 등록, 설정 로딩, 인증 토큰, 라우팅 헬스체크 |
| 2 | `master-data`, `governance` | 기준정보/승인 관리부터 제한 오픈 | 계정/부서/거래처 SCD2 기준 확정, 변경 승인/감사 로그 확인 |
| 3 | `journal-ledger:core`, `journal-ledger:api`, `journal-ledger:batch` | 전표/원장 엔진을 내부 트래픽 또는 shadow mode로 적용 | 전표 생성, 승인, 전기, GL/SL 반영 경로가 하나로 수렴 |
| 4 | `tax` | 세금계산서 등록/조회부터 운영 적용 | `PURCHASE`/`SALES` 타입 검증, 금액 합계 검증, 지출/AP 연계 테스트 |
| 5 | `receivable`, `payable` | AR/AP 서브레저를 파일럿 법인/부서에 적용 | 원천 문서 드릴다운, 전표 lineage, 잔액 상태 전이 테스트 |
| 6 | `expenditure-resolution` | 지출결의/승인/AP 지급 흐름 적용 | 예산 차감 정책, 세금계산서 연계, 승인 후 전표 생성 검증 |
| 7 | `asset-lease` | 고정자산부터 적용 후 IFRS16 리스 적용 | 취득/상각/처분 검증, IFRS16 이자/원금 분리 결의 검증 |
| 8 | `loan` | 운영 전 추가 보강 후 제한 적용 | 실행→이연→3개월 상각→중도상환 재계산 회귀 테스트 필요 |
| 9 | `closing`, `reconciliation` | 처음에는 읽기/검증 모드, 이후 마감 통제 활성화 | 원천-전표-원장 대사율, 마감 실패/재오픈 절차 검증 |
| 10 | `reporting` | 보고서 shadow run 후 공식 보고 전환 | 보고 라인에서 원천 문서까지 drill-through 검증 |

현재 코드 상태 기준으로는 `master-data -> journal-ledger -> tax -> payable/receivable -> expenditure-resolution -> asset-lease`까지가 우선 적용 후보이고, `loan`, `closing`, `reconciliation`, `reporting`은 운영 전 회귀 테스트와 전기/대사 수렴 검증을 더 보강하는 것이 안전하다.

## 2.2 운영 적용 게이트

각 단계는 다음 조건을 통과한 뒤 다음 단계로 넘어간다.

- 컴파일/테스트: 해당 모듈 `compileJava` 또는 `test`가 성공한다.
- 데이터 정합성: 원천 문서 ID, 전표 lineage, GL/SL 반영 결과가 추적된다.
- 재처리: 실패 후 재시도해도 중복 전표나 중복 잔액 차감이 발생하지 않는다.
- 롤백: 설정/라우팅/기능 플래그로 신규 트래픽을 차단할 수 있다.
- 감사: 변경자, 승인자, 적용일, 원천 문서가 로그 또는 엔티티로 남는다.
- 운영 관찰: 헬스체크, 주요 오류 로그, 처리 건수/금액 지표를 확인할 수 있다.

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
