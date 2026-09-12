# GH-665 명령 멱등성 계약과 검증

[Issue #665](https://github.com/skyg547/account/issues/665)의 작업 계약이다.
2026-09-12 사용자 구현 요청과 Full Authority에 따라 부모 Integrator가 legacy 거부안을 채택했으며,
[원격 정책·시작 기록](https://github.com/skyg547/account/issues/665#issuecomment-5645801578)에
호환성 영향과 구현·검증 범위를 남겼다. 이는 과거 제안이 이미 승인되어 있었다는 뜻이 아니다.
완성된 여섯 명령과 저장·HTTP 경계를 하나의 atomic Draft PR로 제출하며 사람의 통합 리뷰를 받는다.

## 추적과 수정 전 사실 (base 87206771)

- Base: `origin/main@87206771d8057fdd4f39bd35df0331768d0bf05f`.
- Branch: `agent/665-command-idempotency`; worktree: `/tmp/account-665-command-idempotency`.
- #666/PR #705는 main에 병합됐다. 운영평가 수치 검증과 기존 회귀를 보존한다.
- `RcmService`와 `EvaluationService`의 여섯 쓰기 메서드는 업무 저장 후 감사 append를 호출한다.
- `AuditLogPersistenceAdapter.append`는 같은 키가 있으면 actor/action/대상/details 비교 없이 반환한다.
- 두 V61은 `idempotency_key` 단독 UNIQUE다. 기존 namespace는 모듈 전체다.
- `detailsJson`은 저장 결과이며 직렬화 실패 시 문자열 fallback도 가능하다. 원래 요청의 증거가 아니다.
- 기존 `AuditTrailEndToEndIntegrationTest`의 retry는 업무 명령이나 HTTP 재호출이 아니라 감사 포트만 호출한다.

## 적용 정책

**Legacy 정책:** 감사 로그만 있고 검증된 receipt가 없는 키는 업무 저장 전에 HTTP 409로 거부하고,
fingerprint와 최초 결과 snapshot을 함께 가진 새 receipt만 재생한다.
과거 정상 요청의 재시도도 409가 되는 호환성 변경이다. 기존 details를 receipt로 자동 승격하지 않는다.
중복 업무 생성 위험이 있으므로 새 키로 일괄 재전송하도록 안내하지 않는다.

다음 계약을 적용한다.

| 경계 | 처리 |
| --- | --- |
| 키 없음/null/빈 문자열/공백 | 현재 `AuditActorContext` 규칙대로 키 없음으로 처리. 매 유효 명령을 실행하고 매번 감사 기록을 추가하며 receipt를 만들지 않는다. |
| 비어 있지 않은 키 | 기존 trim과 모듈 전체 namespace를 유지한다. actor/action별 namespace를 만들지 않는다. |
| 같은 키, 같은 유효 명령 | 최초 성공 결과 snapshot을 반환. 업무·감사·receipt를 다시 저장하지 않는다. |
| 같은 키, actor/action/대상/유효 payload 변경 | HTTP 409와 고정된 충돌 코드. 저장된 요청·actor·snapshot 내용을 응답에 노출하지 않는다. |
| legacy key | details가 null/JSON/비JSON인지와 무관하게 HTTP 409. 업무·감사 row 불변. |
| 다른 키의 유효 명령 | 실행과 새 감사 기록을 하나의 트랜잭션으로 저장한다. |
| 다른 키의 갱신 후 과거 키 재시도 | 과거 결과만 재생하고 최신 업무 상태를 되돌리지 않는다. |

충돌 응답은 `409 {"code":"IDEMPOTENCY_CONFLICT","message":"Idempotency key cannot be reused for this command."}`다.
receipt 내부 상태 실패는 `500 {"code":"INTERNAL_AUDIT_ERROR","message":"Unable to process the command."}`이며 원인이나 저장 내용을 공개하지 않는다.
키 길이는 trim 이후 255자를 넘으면 저장 전에 400으로 거부한다.

HTTP 인증/역직렬화/기본 입력 검증은 계속 적용한다. 최초 성공 재생은 현재 부모 row의 상태를 다시 검사하거나
업무 저장을 수행하지 않는다. timestamp와 correlation ID는 명령 동일성에서 제외하고 최초 감사 lineage를 보존한다.
새 요청에 본래 있던 유효한 API status/body 계약을 snapshot 재생에도 유지한다. 동적 HTTP 추적 헤더는 snapshot 범위가 아니다.

## 동일성 및 저장 경계

- fingerprint v1은 action, 신뢰 actor, aggregate type/ID, 적용 path, 정규화된 명령 payload를 포함한다.
  원시 JSON 문자열을 비교하지 않는다. 고정 field 순서와 명시적 null을 가진 canonical 표현과 버전을 저장한다.
- RCM의 owner 기본값, risk/control의 path 부모 ID, 평가의 신뢰 evaluator와 result 정규화,
  #666의 독립적인 null 수치를 반영한다. 문자열을 임의로 trim하거나 배열 순서를 정렬해 의미를 바꾸지 않는다.
- snapshot v1은 저장 포트가 반환한 여섯 도메인 결과의 전체 응답 필드를 보존한다.
  명령 receipt 직렬화/복원 실패에는 `String.valueOf` fallback을 쓰지 않고 실패시킨다.
  replay는 최초 감사 이벤트의 actor/action/aggregate와 snapshot JSON의 일치도 확인한다.
  v1은 현재 여섯 record의 전체 필드를 고정한다. 향후 필드 추가/변경은 명시적인 버전·기존 receipt 호환성 결정을 요구하며,
  기존 snapshot에 새 기본값을 추정하여 넣지 않는다. HTTP의 들여쓰기/root wrapping/naming/null 생략 설정은 persisted v1에 적용하지 않는다.
  손상된 snapshot, 불완전한 완료 receipt, 지원하지 않는 fingerprint/snapshot 버전은
  업무를 재실행하거나 현재 업무 상태로 대체하지 않고 내부 오류로 실패한다.
- application이 트랜잭션을 소유하고 별도 outbound port/adapter가 receipt 예약·완료·조회를 담당한다.
  append-only 감사 포트에 mutable receipt 책임을 넣지 않는다.
- reservation, 업무 저장, 감사 append, snapshot 완료는 같은 트랜잭션이다.
  업무/감사/receipt 각각의 실패 후 durable row와 완료되지 않은 reservation이 남지 않아야 한다.
- 최초 키 경합을 DB에서 직렬화한다. 사전 조회만으로 보호하거나 UNIQUE 실패 후 rollback-only 트랜잭션에서
  재조회하여 복구하지 않는다. `floorMod(trimmedKey.hashCode(), 256)`으로 미리 만든 lock row를
  `SELECT ... FOR UPDATE`로 잠근다. 같은 키의 명령과 direct append가 같은 lock에 참여한다.
  해시가 같은 다른 키는 대기할 수 있지만 receipt의 PK는 전체 키이므로 의미가 합쳐지지 않는다.
  동시에 여러 키를 쓰는 외부 트랜잭션은 잠금 순서에 따라 deadlock으로 전체 rollback될 수 있다.
- direct audit append도 동일 이벤트만 중복 제거하고 다른 이벤트에는 방어적 충돌을 내야 한다.
  direct append와 명령 사이의 같은 키 경합도 포함한다. 기존 V61과 감사 row는 수정하지 않는다.
  이전 버전 서비스는 새 reservation에 참여하지 않으므로 혼합 버전 쓰기를 허용한 채 보장을 주장하지 않는다.
  실제 배포 전 쓰기 전환/일시 중지 절차를 별도로 확인한다.

## 구현 단위와 소유권

하나의 atomic PR에 아래 구현 단위를 통합한다. 완성되지 않은 기반을 별도 병합하지 않는다.
동시 작성자는 파일을 나누고 공용 기록·빌드·Git 상태는 부모 Integrator가 소유한다.

| 순서/소유자 | 파일과 실행 단위 |
| --- | --- |
| A / Service + SQL + Test | 신규 `CommandReceipt`, 충돌 예외, `CommandReceiptPort`, `IdempotentCommandExecutor`; 실제 receipt adapter와 H2/PostgreSQL 신규 migration; canonical/예약/트랜잭션 테스트를 함께 완성한다. |
| B / Service + Test | `RcmService.java`, `RcmServiceTest.java`: A 이후 세 RCM 명령과 재생/충돌 적용. |
| C / Service + Test | `EvaluationService.java`, `EvaluationServiceTest.java`: A 이후 세 평가/결함 명령 적용. #666 검증 보존. |
| 통합 / Controller + Test | `InternalAuditApiExceptionHandler.java`와 해당 테스트, 기존 감사 통합 테스트 및 새 HTTP/트랜잭션/동시성 테스트. 공용 executor와 HTTP mapping은 각각 단일 소유자. |
| 문서 / 부모 Integrator | 모듈 README/process-flow/schema와 이 정책안; harness 3개 및 history. |

신규 migration은 모듈 내 V62이며 기존 V60/V61을 수정하지 않는다.
필요한 core/API 빌드 설정, migration-runner 검증 영향까지 사용자 Full Authority 범위에서 포함한다.
최종 PR은 연결된 여섯 명령 계약이 완성되고 검증된 뒤 `Refs #665`로 Draft 생성한다.

## 검증 계획과 수용 게이트

1. 여섯 명령 모두 같은 키·같은 명령 재시도, actor/action/대상/payload 각각 변경,
   다른 키 변경 후 과거 키 재시도, 서로 다른 키의 정확한 감사 lineage를 검증한다.
2. legacy null/JSON/비JSON details, null/공백/trim 키, owner/evaluator 기본값, path/body 일치,
   result 정규화와 운영평가 null/0 수치 경계를 확인한다. 손상·불완전 receipt와 미지원 버전은
   재실행/현재 상태 fallback 없이 실패하고 업무·감사·receipt가 불변인지 검증한다.
3. 실제 Spring/H2 트랜잭션과 HTTP를 사용한다. 충돌 시 업무/감사/receipt가 불변인지 별도 트랜잭션으로 조회한다.
   업무/감사/receipt 저장 및 snapshot 직렬화 실패를 각각 주입하고 rollback을 확인한다.
4. 격리된 임시 PostgreSQL 16에서 신규 migration/validate와 같은 최초 키 동시 호출을 barrier로 겹친다.
   동일 명령은 실행 1회/결과 동일, 상이한 명령은 성공 1회/충돌 1회, loser의 업무/감사 부작용 0을 확인한다.
   실패한 winner 이후 재시도와 direct append 경합도 검증한다. 기존 개발/운영 DB에 접근하지 않는다.
5. JDK17/Gradle8.7로 `sh ./gradlew :internal-audit:core:test :internal-audit:api:test --offline --no-daemon --console=plain --max-workers=1` 실행 후 API bootJar/local H2 실기동을 확인한다.
   빌드·migration 공용 계약이 바뀌면 해당 runner/정책 테스트를 추가한다.
6. 독립 Reviewer가 Q1–Q4와 실제 diff/검증 증거를 검사한다. H2/Mockito 성공을 PostgreSQL 정합성으로 대체하지 않는다.

## 롤백과 권한 분리

검토된 코드 변경을 revert하고 감사 데이터는 보존한다. 신규 migration 파일이나 적용 이력은 삭제·수정하지 않는다.
이전 애플리케이션으로의 롤백은 명령 보호를 잃을 수 있으므로 운영 복귀 정책도 리뷰한다.
배포, 과거 데이터 복구, 인증체계 변경은 이 수정의 비목표다.

Implementer tier: `difficulty:very-high`, 사용자 지정 reasoning `xhigh`.
리뷰어는 읽기 전용이며 구현과 분리한다. Git/GitHub·공유 기록은 부모 Integrator만 변경한다.
Merge authority: 독립 검수와 최신 head/base·CI 확인 후 별도 승인된 부모 Integrator.
요청된 종료점은 검증된 Draft PR이며 Ready·merge·Issue close는 별도 게이트다.


## 격리 PostgreSQL 실행 방법

기존 DB를 대상으로 실행하지 않는다. 아래 fixture는 기존 캐시 image만 사용하며, network-none pod의
loopback과 tmpfs DB를 테스트 컨테이너만 공유한다. 실제 계정정보 없이 합성 `account_test`/빈 암호를 쓴다.
`postgresTest`는 지정하지 않으면 실패하며 기본 `test`에서는 PostgreSQL tag를 제외한다.
Podman은 rootless로 실행하고 다른 컨테이너·volume은 정리하지 않는다.

```bash
podman pod create --name account-665-verification --network none
podman run --pull=never -d --rm --name account-665-postgres --pod account-665-verification --cpus=0.5 --memory=384m --pids-limit=128 --tmpfs /var/lib/postgresql/data:rw,size=256m -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_USER=account_test -e POSTGRES_DB=account_idempotency_test docker.io/library/postgres:16-alpine
podman exec account-665-postgres pg_isready -U account_test -d account_idempotency_test
# Replace the two bind paths with this checkout and an existing Gradle cache.
podman run --pull=never --rm --name account-665-postgres-check --pod account-665-verification --user 0:0 --cpus=1 --memory=1536m --pids-limit=512 -v /home/ho/.gradle:/home/gradle/.gradle -v /tmp/account-665-command-idempotency:/workspace -w /workspace -e GRADLE_USER_HOME=/home/gradle/.gradle docker.io/library/gradle:8.7-jdk17-alpine sh ./gradlew :internal-audit:api:postgresTest -DinternalAudit.test.postgresql.url=jdbc:postgresql://127.0.0.1:5432/account_idempotency_test --offline --no-daemon --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m -XX:ActiveProcessorCount=1'
# After the test container has exited, remove only this disposable fixture.
podman stop account-665-postgres
podman pod rm account-665-verification
```

PostgreSQL 동시성 검증은 기본 `READ_COMMITTED` 격리 수준을 대상으로 한다.
다른 격리 수준은 serialization 실패로 전체 rollback될 수 있으며 별도 검증이 필요하다.
Local H2가 PostgreSQL/TLS/경합의 대체 증거는 아니다. 이 fixture도 운영 TLS, 실제 인증 통합,
혼합 버전 배포 또는 부하 한계를 검증하지 않는다.


## 2026-09-12 검증 결과

| 검증 | 결과 |
| --- | --- |
| JDK17 core | 172 PASS |
| JDK17 API/H2 | 232 PASS (새 명령/트랜잭션 177 포함) |
| 격리 PostgreSQL16 | 214 PASS (공통 177 + 실제 잠금 경합 36 + migration/READ_COMMITTED 1) |
| migration-runner | 140 PASS, V61 기존 감사 보존 및 V62 세 경로 동등성 포함 |
| 실행 JAR / 실제 HTTP | H2 V62 기동, 26개 점검 PASS |
| 독립 리뷰 | P0–P3 없음, Q1–Q4 PASS |

Java 합계758건의 failures/errors/skips는0이다. 실행 JAR의 HTTP 검증은 여섯 명령의 재생 및 process의
409·과거 재생/최신 상태 보존을 확인했다. 여섯 명령 전체의 충돌/rollback은 H2·PostgreSQL 통합 테스트에서 검증했다.
테스트 전용 `--management.health.redis.enabled=false`를 사용했으며 기존 개발/운영 DB는 접근하지 않았다.
하네스 정책 검사32건과 diff/marker 검사도 통과했다. 실제 배포와 최신 PR head의 CI/사람 리뷰는 별도 게이트다.
