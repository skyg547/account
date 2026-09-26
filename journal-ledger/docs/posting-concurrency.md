# 전표·잔액의 동시 전기와 재시도

전기(Posting)는 승인 전표 한 건을 GL/SL과 잔액에 반영하는 작업입니다. 같은 전표 ID에
두 요청이 겹쳐도 경제적 효과는 한 번이어야 합니다. 처리자를 생략한 canonical `system` 요청과
명시 처리자 요청, JPA와 `jdbc-bulk` 모드 모두 같은 `PostingService`를 통과합니다.

## 입력에서 커밋까지

1. `PostingService.postJournalEntry(id, poster)`가 쓰기 트랜잭션을 시작합니다.
2. `JournalPersistenceAdapter.findByIdWithDetails`는 같은 트랜잭션의 미반영 생성/승인을
   먼저 flush하고 `SELECT id FROM journal_entries WHERE id = ? FOR UPDATE`로 헤더 한 행을 잠급니다.
   PostgreSQL의 nullable LEFT JOIN 잠금 제약을 피하려고 상세 JOIN에는 잠금을 붙이지 않습니다.
3. 잠금 획득 후 관리 중인 전표도 refresh하여 최신 상태와 상세를 읽습니다. 잠금 대기 전에
   이미 읽은 `APPROVED` 객체가 남아 있어도, 먼저 커밋한 요청의 `POSTED`를 확인합니다.
4. 기존 승인 상태, 거래/기준통화 차대일치, 저장된 상세 ID와 마감 검증을 그대로 거칩니다.
   성공하면 같은 불변 스냅샷으로 GL/SL을 저장하고 잔액을 갱신합니다.
5. 헤더 잠금은 전체 트랜잭션의 커밋/롤백까지 유지됩니다. 실패하면 `POSTED`와 감사 사용자,
   GL/SL, 잔액이 함께 롤백되며 다음 요청이 다시 전기를 시도할 수 있습니다.

완료 요청을 다시 보내거나 동시 요청에서 뒤에 잠금을 얻은 요청은
`IllegalStateException("승인된 전표만 원장으로 전기할 수 있습니다.")`로 거부합니다.
`POST /api/journals/{id}/post`의 기존 HTTP 400 응답 계약을 유지하며 추가 금액은 반영하지 않습니다.
응답 유실 후 재요청도 같은 결과입니다. 조회로 최종 `POSTED` 상태를 확인할 수 있습니다.

`findByIdWithDetails`의 포트/생성자 시그니처는 유지됩니다. 일반 상세 조회는 자체 트랜잭션
동안만 짧게 헤더를 잠그며, 명시적인 읽기 전용 트랜잭션은 잠금 없이 JOIN FETCH합니다.
전기는 쓰기 트랜잭션에서 호출해야 합니다. 쓰기 조회는 잠금과 refresh를 위한 추가 쿼리가
발생하고 같은 ID 요청은 대기합니다. 기간 조회 지연도 이 잠금 보유 시간을 늘립니다.
서로 다른 전표의 잔액 경쟁은 다음 공통 계정 잠금으로 처리합니다. 분산 마감 경쟁은 별도 통제입니다.

## 서로 다른 전표가 같은 잔액을 갱신할 때

초보자 예: 두 전표가 각각 차변 100.00과 40.00을 반영할 때 둘 다 이전 잔액 0.00을
읽으면 마지막 저장만 남아 100.00 또는 40.00이 됩니다. 두 번째 전표가 첫 번째 커밋 후
최신 100.00을 읽어야 최종 140.00이 됩니다. 아직 잔액 행이 없거나 SL의 거래처·부서가
NULL이어도 같은 순서가 필요합니다.

1. `LedgerService.updateLedgerBalancesBulk`는 모든 상세의 계정·통화를 먼저 모읍니다.
   단건 변경도 같은 출력 포트 `lockBalanceAccounts`를 사용합니다.
2. JPA/JDBC 두 어댑터의 `LedgerBalanceWriteLock`은 실제 연결이 `READ_COMMITTED`이고
   활성 쓰기 트랜잭션인지 확인합니다. `REPEATABLE_READ`/`SERIALIZABLE`은 잠금 대기 전에
   형성된 snapshot이 refresh 후에도 오래될 수 있으므로 잔액 쓰기를 시작하지 않고 거부합니다.
3. V14가 미리 만든 256개 행 중 필요한 번호를 중복 제거하고 오름차순으로 `FOR UPDATE`
   잠급니다. 번호는 `floorMod(31 * accountCode.hashCode() + currencyCode.hashCode(), 256)`입니다.
   서로 다른 계정이 같은 번호를 가져도 금액 키는 그대로이며 잠금 대기만 늘어납니다.
   날짜와 SL 차원은 번호에 넣지 않아 이월 조회와 NULL 차원도 같은 계정 잠금에 포함됩니다.
4. 잠금 전에는 같은 트랜잭션의 이전 작업을 flush하고, 잠금 후 잔액을 조회할 때는
   관리 중인 객체도 refresh합니다. 따라서 이전에 읽어 둔 오래된 JPA 객체를 재사용하지 않습니다.
   JDBC는 refresh한 잔액 객체를 detach하여 이후 JPA 자동 flush가 bulk SQL 결과를 덮어쓰지 않게 합니다.
5. bulk 입력 날짜를 오름차순으로 처리하고 기존 도메인 `BigDecimal` 검증, 이전 기말 이월과
   `기초 + 차변 - 대변` 계산을 수행합니다. GL/SL 그룹 키는 record 값 비교를 사용하여 실제 NULL,
   문자열 `"NULL"`, 구분 문자가 포함된 코드를 서로 다른 차원으로 보존합니다.
6. 계산된 절대 금액을 기존 JPA `saveAll` 또는 JDBC GL upsert/SL null-safe update+insert로
   저장합니다. 잠금은 전표 상태·엔트리·잔액을 포함한 전체 트랜잭션 커밋/롤백까지 유지됩니다.
7. 정상 전기는 날짜·GL 키와 정확한 nullable SL 키마다 `차변-대변` delta를 계산하고,
   `balance_date`가 전기일보다 큰 기존 행의 기초·기말을 set-based UPDATE로 함께 이동합니다.
   JPA/JDBC-bulk 어댑터 모두 UPDATE 전에 flush하고 뒤에 1차 캐시를 비우므로 bulk SQL 이전
   객체가 후속 계산이나 flush에서 결과를 덮지 않습니다. 재집계 replay는 이 전파를 끕니다.

단일 호출은 모든 잠금을 먼저 같은 순서로 확보하므로 전표 상세의 계정 순서가 달라도
잠금 순서가 뒤집히지 않습니다. 다만 한 외부 트랜잭션에서 여러 전표를 순차 전기하거나
다른 업무 자원도 잠그면 데드락이 발생할 수 있습니다. DB가 중단시킨 트랜잭션은 예외를
그대로 전파하며 내부에서 재시도하지 않습니다. **호출자는 해당 외부 트랜잭션 전체를 롤백한 뒤
새 트랜잭션으로 전기를 다시 요청**해야 합니다. 잔액 메서드만 다시 실행하거나 실패한
트랜잭션을 계속 사용하면 안 됩니다. 이미 커밋한 전표 재요청은 헤더 상태 검증이 거부합니다.
`LedgerService` 자체는 금액을 누적하는 내부 연산이므로 성공한 delta를 임의 재호출하는
멱등 API가 아닙니다.

모든 잔액 writer는 같은 잠금 계약을 지켜야 합니다. `saveGlBalances`/`saveSlBalances`만
직접 호출하거나 DB에 절대 금액을 직접 쓰는 외부 writer까지 자동 보호하지 않습니다.
256개 번호는 잠금 자원 사용량을 제한하는 대신 무관한 계정의 충돌도 허용합니다. 실제 부하에서
대기 시간과 트랜잭션 길이를 측정해야 하며 번호 수/매핑을 서비스별로 다르게 설정하면 안 됩니다.

### 재집계와 이월 경계

`reaggregateLedgerBalancesForPeriod`는 한 트랜잭션 안에서 256개 잠금을 모두 잡은 뒤
기간 잔액 삭제 → 커밋된 POSTED 상세 조회 → 날짜순 재생성을 수행합니다. 전기가 먼저
잠금을 얻으면 재집계는 그 커밋을 포함하고, 재집계가 먼저면 대기한 전기가 재생성 후 잔액에
추가됩니다. 직접 cleanup은 완성되지 않은 결과를 공개하므로 이제 거부됩니다.

`dailyBalanceReaggregationJob`은 다음 fail-closed 프로토콜을 사용합니다.

1. start Step이 JobParameter의 `startDate/endDate`, 별칭, `baseDate/targetDate`를 정규화합니다.
   256개 stripe를 먼저 획득한 뒤 요청 종료일, GL/SL 최종 잔액일, 최신 `POSTED` 회계일의
   최댓값으로 유효 종료일을 확장합니다. V15 singleton을
   `REBUILDING(owner JobInstance ID, effective range)`으로 바꾸고 같은 유효 범위를
   JobExecutionContext에 문자열로 고정하며 epoch를 증가시킵니다.
2. owner cleanup과 100-detail chunk만 닫힌 상태에서 쓸 수 있습니다. 성공한 cleanup은 같은
   JobInstance 재시작에서 다시 실행되지 않고, chunk의 저장과 reader checkpoint는 같은
   트랜잭션으로 커밋됩니다. 실패 listener/`afterJob`은 제어를 열지 않습니다.
3. 일반 전기는 영향 계정 stripe를 잡은 뒤 `OPEN`을 확인하므로 start보다 먼저 온 전기는
   POSTED 입력에 한 번 포함되고, 나중 전기는 상태/엔트리/잔액 전체가 롤백됩니다.
4. 마지막 Step은 모든 stripe를 다시 잡고 owner를 확인한 뒤 안정된 POSTED source와 GL/SL을
   날짜, 전체 key, nullable BP/부서, 일별 차변/대변, 기초/기말까지 대사합니다. 누락·추가·금액
   불일치는 트랜잭션을 롤백해 계속 `REBUILDING`으로 남깁니다. 일치할 때만 `OPEN`으로 바꾸고
   epoch를 다시 증가시킵니다.

잔액 조회는 긴 shared lock 대신 조회 전 `OPEN+epoch`, materialize/집계 후 같은
`OPEN+epoch`를 확인합니다. 중간에 재집계가 시작되거나 끝났으면 결과를 반환하지 않습니다.
직접 DB SQL과 barrier를 모르는 구버전 writer는 이 통제를 우회하므로 혼용할 수 없습니다.

정상 과거일 전기는 기존 후속일 행을 같은 트랜잭션에서 이동하므로 커밋 뒤 stale projection을
공개하지 않습니다. delta가 0인 키는 UPDATE하지 않고, 거래가 없던 날짜의 행은 만들지 않습니다.
배포 전에 이미 stale한 데이터, 직접 SQL/구버전 writer의 결과와 기존 중복·손상 데이터는 자동
탐지하거나 보정하지 않으므로 가장 이른 영향일부터 승인된 재집계를 실행해야 합니다.
[이월 규칙](ledger-carry-forward.md)을 따릅니다.

후속일 UPDATE는 Java에 여러 해의 잔액을 적재하지 않지만, 오래된 전기 한 건이 해당 키의 많은
행을 갱신하여 row lock, WAL/undo, 복제 지연과 트랜잭션 시간을 늘릴 수 있습니다. 계정·통화
stripe도 커밋까지 유지되므로 운영 분포의 실행 계획과 lock wait를 배포 전에 측정해야 합니다.
직접 SQL writer와 포트를 우회한 저장은 이 전파·OPEN·cache 보호를 모두 우회합니다.

## V14 업그레이드와 롤백

다른 업무 모듈에 내장된 journal core까지 포함하여 모든 전기·재집계 writer를 중지한
배포 창에서 V14를 적용하고, 모든 인스턴스를 같은 잠금 규칙의
새 버전으로 전환한 후 재개합니다. 잠금을 사용하지 않는 구버전 writer와 혼용하면 안전하지 않습니다.
V14는 업무 데이터 수정 없이 `ledger_balance_locks(lock_id)` 256행만 생성합니다.
runtime role은 이 테이블의 `SELECT`와 `UPDATE` 권한이 있어야 `FOR UPDATE`를 실행할 수 있습니다.

승인된 migration 환경에서 다음 결과가 `256, 0, 255`인지 확인합니다.

```sql
SELECT COUNT(*), MIN(lock_id), MAX(lock_id) FROM ledger_balance_locks;
```

잠금 행이 누락되거나 V14가 적용되지 않았으면 전기는 실패하고 전체 트랜잭션을 롤백합니다.
기존 GL/SL 금액과 V13 제약은 유지되며 과거 중복 SL 잔액을 임의 합치지 않습니다.
롤백 시에도 모든 writer를 중지한 뒤 검토된 코드 revert를 적용하고 V13/V14는 유지합니다.
이전 코드는 잔액 잠금 보호가 없으므로 수정 버전으로 정합성 보호를 회복할 때까지 전기·재집계
트래픽을 중지한 상태로 유지합니다. migration 파일 삭제/repair로 적용 이력을 바꾸지 않습니다.

## V15 업그레이드, 장애 운영과 롤백

모든 journal-ledger writer를 중지한 배포 창에서 V15를 먼저 적용하고, API/Batch/이 모듈 core를
내장한 인스턴스를 모두 같은 버전으로 교체한 뒤 시작합니다. `ledger_reaggregation_control`은
정확히 한 행이어야 하며 runtime role은 SELECT/UPDATE가 필요합니다.

장애 후 `REBUILDING`이면 행을 수동 OPEN으로 바꾸거나 새 JobInstance를 만들지 않습니다.
동일한 식별 JobParameters로 같은 JobInstance를 재시작하여 저장된 기간/checkpoint와 출력이
계속 맞도록 합니다. 최종 대사 실패는 원천·잔액을 조사한 뒤 같은 인스턴스를 재개합니다.
코드 롤백도 모든 writer를 중지한 상태에서 수행하며 V15 migration과 제어 행은 보존합니다.
구버전은 barrier를 사용하지 않으므로 복구 전까지 전기/재집계/잔액 조회 트래픽을 열지 않습니다.

후속일 전파 코드는 새 migration을 추가하지 않지만 모든 posting writer가 같은 버전이어야 합니다.
구버전과 혼용하면 구버전에서 수행한 과거일 전기가 다시 stale 잔액을 만들 수 있습니다. 배포 전
writer를 멈추고 기존 부정합을 대사한 뒤, 필요하면 가장 이른 영향일부터 확장 재집계를 완료하고
전 인스턴스를 함께 기동합니다. 코드 롤백도 writer를 먼저 중지하며 이미 전파된 올바른 delta를
역으로 빼지 않습니다. 롤백 버전으로 트래픽을 재개해야 한다면 후속 과거일 전기를 금지하고,
수정 버전 복구 후 같은 범위의 재집계·대사가 끝날 때까지 잔액을 권위 값으로 공개하지 않습니다.
로컬 H2 회귀는 실제 PostgreSQL의 대량 UPDATE 비용, WAL/복제 지연, 분산 lock timeout을 대신하지 않습니다.

## V17 역분개 작업 업그레이드와 롤백

역분개 생성은 원본 `POSTED` 헤더를 `FOR UPDATE`로 잠그고, 원본 ID가 PK인
`journal_reversal_operations`의 현재 관계를 확인합니다. 순차·동시 재요청은 첫 작성자가
만든 `PENDING` 또는 이미 `POSTED`된 같은 역분개를 반환합니다. 새 slip 생성과
operation 저장은 하나의 쓰기 트랜잭션이므로 중간 실패는 둘 다 rollback하고
원본의 역분개 권리를 소진하지 않습니다.

전기와 취소는 역분개 전표 헤더 잠금을 경쟁 판정자로 사용합니다. 취소는
operation에서 현재 전표 ID와 terminal 상태를 먼저 확인한 뒤 그 전표를 잠그고 refresh합니다.
전기가 먼저이면 전표·GL/SL·잔액·operation `POSTED`가 함께 commit되고, 대기하던 취소는
최신 `POSTED` 전표에서 operation을 바꾸기 전에 거부됩니다. 취소가 먼저이면 전표
`REJECTED`와 operation `CANCELLED`가 함께 commit되고 뒤의 전기가 거부됩니다. lock timeout,
deadlock, serialization failure 후에는 중단된 트랜잭션을 계속 사용하지 말고 새
트랜잭션으로 전체 명령을 재시도합니다.

V17은 인식 가능한 기존 역분개를 backfill하며 부정합을 임의로 삭제·선택하지
않습니다. 배포 절차는 다음과 같습니다.

1. 구·신 writer를 모두 중지하고, `REVERSAL` + `JOURNAL_ENTRY` lineage의 비숫자
   원본 ID, orphan, `POSTED`가 아닌 원본, 원본당 복수 활성 역분개를 대사합니다.
2. migration 전용 권한으로 V17을 migrate/validate합니다. 비숫자 cast, FK, PK/UNIQUE,
   lifecycle CHECK 중 하나라도 위반하면 적용은 fail-closed로 중단되어야 합니다.
3. `journal_entries(id, status)` UNIQUE 생성은 기존 행 스캔·index build·DDL lock·추가
   디스크를 유발할 수 있습니다. 사전 복제본에서 시간과 용량을 측정하고,
   완료될 때까지 writer 중지 배포 창을 유지합니다.
4. runtime role에 새 테이블의 SELECT/INSERT/UPDATE만 운영 정책에 맞게 부여하고,
   Hibernate schema validate를 통과한 뒤 새 코드를 기동합니다.
5. 배포 후 순차·동시 중복, 취소 후 재생성, 전기-취소 경쟁, `POSTED` 순효과
   재집계를 폐기 가능한 테스트 데이터로 확인합니다.

실패 롤백은 writer 중지 후 애플리케이션 코드를 이전 버전으로 돌리는 방식입니다.
이미 성공한 V17 파일을 수정·삭제하거나 Flyway `repair`로 덮지 않고,
`journal_entries(id, status)` UNIQUE, 원본 `POSTED` 복합 FK, operation 테이블과 backfill
관계를 모두 보존합니다. 단, V17 이전 애플리케이션은 operation을 읽지 않으므로
코드를 돌렸다는 이유만으로 역분개 트래픽을 재개하지 않습니다. 수정 버전 또는
승인된 forward migration으로 보호를 회복할 때까지 역분개 생성·승인·전기 writer를
비활성화하고, 이 writer를 분리할 수 없으면 영향받는 전체 journal writer를 중지합니다.
롤백 창에서 생성된 `REVERSAL` 전표는 자동 backfill되지 않으므로, 재개 전에
원본·역분개·GL/SL·operation을 대사하고 승인된 조정/후속 migration으로 관계를 복구합니다.
항목별 backfill 규칙과 제약은 [데이터 모델](schema.md#역분개-작업-v17)을
참고합니다. 직접 SQL writer, 운영 PostgreSQL lock wait/부하, 분산 장애, 기존 부정합의
자동 복구는 이 변경의 검증 범위 밖입니다.

## V13 업그레이드

새 `V13__unique_journal_posting.sql`은 GL/SL 각각의 `journal_detail_id`에 `NOT NULL`과
고유 제약을 추가합니다. 기존 V1/V10/V11/V12의 체크섬은 바꾸지 않습니다. 서비스 경로를
우회한 중복 adapter 저장도 DB가 거부하며, 잘못된 이력은 자동으로 삭제하거나 합산하지 않습니다.

배포 소유자가 승인된 점검 환경에서 다음 쿼리로 기존 위반 여부를 확인합니다. 결과가 없으면
상세 참조별 중복/NULL 위반이 없다는 뜻입니다.

```sql
SELECT journal_detail_id, COUNT(*) FROM gl_entries
GROUP BY journal_detail_id HAVING journal_detail_id IS NULL OR COUNT(*) > 1;
SELECT journal_detail_id, COUNT(*) FROM sl_entries
GROUP BY journal_detail_id HAVING journal_detail_id IS NULL OR COUNT(*) > 1;
```

위반이 있으면 V13은 실패하여 배포를 멈춥니다. 회계 담당자가 원천 전표, GL/SL, 잔액과
감사 이력을 대조하고 별도로 승인한 정정 절차를 완료한 후 다시 마이그레이션합니다.
제약 생성은 테이블 검사와 DDL 잠금이 필요하므로 대량 이력이 있는 환경은 별도 배포 창과
소요 시간을 확인해야 합니다. PostgreSQL의 실패한 transactional migration은 롤백됩니다.
H2는 일부 DDL이 남을 수 있으므로 테스트용 DB를 새로 만들어 검증합니다.

롤백은 전기 트래픽을 중단한 상태에서 검토된 애플리케이션 revert로 수행합니다. V13 고유
제약은 유지해야 이전 코드로 돌아가도 중복 원장 삽입이 차단됩니다. 적용된 migration 파일을
삭제하거나 기록을 수동으로 바꾸지 않습니다. 배포와 회계 이력 복구는 이 변경의 자동 작업이 아닙니다.

## 회귀 실행과 기대 결과

전제는 JDK 17, 저장소의 Gradle wrapper와 의존성입니다. 저장소 루트에서 실행합니다.

```bash
./gradlew :journal-ledger:test
```

Issue #767 로컬 검증에서는 위 집계 명령과 다음 집중 명령을 사용합니다.

```bash
./gradlew :journal-ledger:core:test --tests '*BalanceReaggregationControlMigrationTest' --tests '*BalanceReaggregationServiceTest' --tests '*PostingConcurrencyIntegrationTest' --console=plain --max-workers=1
./gradlew :journal-ledger:batch:test --tests '*BalanceReaggregationBatchConfigTest' --tests '*BatchDateRangeParameterUtilsTest' --console=plain --max-workers=1
```

이 검증은 H2 PostgreSQL mode의 합성 데이터로 cleanup 직후, 202-detail의 1·2번째 chunk 뒤
장애/동일 instance restart, 겹친 instance, 전기 rollback/retry, epoch read, nullable SL 대사,
대사 실패의 fail-closed 상태를 확인합니다. 실제 PostgreSQL, 운영 데이터, 분산 프로세스 강제
종료와 운영 부하는 별도 검증 대상입니다.

이 집계 task는 core/API/batch 테스트를 모두 실행합니다. 기본 테스트 DB는 독립 H2
PostgreSQL 모드이며 `PostingConcurrencyIntegrationTest`는 각 저장 모드에 대해 다음을 확인합니다.

- 서로 다른 DB 연결 두 개가 같은 `APPROVED` 전표를 먼저 읽은 뒤 겹쳐 전기합니다.
  기존 GL/SL 잔액 행을 미리 만들어 새 키 충돌이 중복 전기를 가리지 않게 합니다.
- 첫 요청을 엔트리 삽입 직전에 잡아 둔 동안 두 번째 요청이 기다리는지 확인합니다.
  결과는 성공 한 번, 상태 충돌 한 번, GL/SL 각 2행, 차변/대변과 잔액 각 100.00입니다.
- 헤더 `POSTED`를 실제 flush한 뒤 엔트리 삽입 전에 오류를 주입합니다. 롤백 후 승인 상태,
  원래 처리자, 0개 엔트리와 기존 잔액을 확인하고 다시 전기에 성공합니다.
- 동일 트랜잭션에서 승인 직후 전기, 읽기 전용 상세 조회, 완료 요청/adapter 재실행을 확인합니다.
  GL뿐 아니라 SL 고유/NOT NULL 제약도 독립적으로 검사합니다.

`PostingIdentityMigrationTest`는 V12에 유효한 원장 이력을 넣고 V13으로 올려 보존/제약을
확인합니다. GL/SL 중복/NULL 이력 네 경우에서는 V13 실패와 원래 금융 이력 보존을 확인합니다.
`LedgerBalanceLockMigrationTest`는 V13의 기존 GL/NULL 차원 SL 금액을 유지하면서 V14의
0~255 잠금 행이 정확히 적용되는지 확인합니다. API/Batch 스키마 검증도 V14까지
적용되었는지 확인합니다.

`LedgerBalanceConcurrencyIntegrationTest`는 서로 다른 전표가 같은 잔액을 변경하는 경우를 검사합니다.

- JPA/JDBC 각각에서 기존/신규 잔액, 거래처/부서의 비NULL·한쪽 NULL·양쪽 NULL 조합을 확인합니다.
- 기존 잔액을 두 연결에서 미리 읽고 첫 번째 갱신을 중간에 멈춥니다. 두 번째 전표는 계정 순서를
  뒤집어 입력해도 기다린 뒤 최신 값에 합산하며, 전표 차변/대변과 GL/SL 금액이 대사됩니다.
- 다음 날 첫 잔액 생성은 앞선 날의 진행 중인 전기를 기다린 뒤 커밋된 기말을 이월합니다.
- 실제 PostgreSQL 데드락으로 한 트랜잭션이 중단되면 상태/감사 사용자/엔트리/잔액이 모두
  롤백되고, 전체 전기를 새 트랜잭션으로 재시도해 한 번의 효과만 남는지 확인합니다.
- 실제 연결의 REPEATABLE_READ에서는 앞서 기록한 POSTED/엔트리까지 롤백되는지 확인합니다.
- 필수 잠금 행 누락 시 전기 전체가 롤백되고, 같은 외부 트랜잭션의 서로 다른 전표 세 건은
  JPA/JDBC 모두 먼저 반영한 금액을 유지하며 누적되는지 확인합니다.

실제 PostgreSQL은 이 테스트 전용으로 만든 disposable PostgreSQL 16 DB에 연결합니다.
예시의 포트는 실행자가 생성한 테스트 인스턴스의 포트로 바꿉니다. 사용자 `postgres`, 비밀번호
없는 테스트 trust 인증을 사용하며 운영 설정 파일이나 자격증명을 읽지 않습니다.
각 테스트 DB fixture는 새 `posting_<random>` 스키마를 만들므로 기존 업무 스키마를 수정하지 않습니다.

```bash
JOURNAL_POSTING_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:44951/posting761 \
  ./gradlew :journal-ledger:core:test \
  --tests '*PostingConcurrencyIntegrationTest' --tests '*PostingIdentityMigrationTest' \
  --tests '*LedgerBalanceConcurrencyIntegrationTest' --tests '*LedgerBalanceLockMigrationTest' \
  --rerun-tasks --offline --max-workers=1
```

PostgreSQL 명령의 기대 결과는 선택한 테스트 모두 통과, 실패/오류/skip 0입니다.
기본 H2 실행도 자체 DB 데드락/롤백을 검사하지만 PostgreSQL의 잠금과 SQLSTATE 검증을 대체하지 않습니다.
PostgreSQL 실행에서는 두 번째 연결의 `pg_stat_activity.wait_event_type = 'Lock'`도 직접
확인합니다. 이것은 독립 연결과 실제 커밋/롤백 검증이며 운영 부하, 프로세스 강제 종료,
분산 장애 주입, 운영 데이터 정합성이나 전체 chunk Job과 온라인 전기의 동시 실행을 검증하지는 않습니다.
실제 실행한 DB/명령/결과는 Issue 작업 기록과 PR 검증 근거를 기준으로 확인합니다.
