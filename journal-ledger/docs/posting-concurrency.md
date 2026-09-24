# 동일 전표의 동시 전기와 재시도

전기(Posting)는 승인 전표 한 건을 GL/SL과 잔액에 반영하는 작업입니다. 같은 전표 ID에
두 요청이 겹쳐도 경제적 효과는 한 번이어야 합니다. 처리자를 생략한 `SYSTEM` 요청과
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
서로 다른 전표가 같은 잔액을 갱신하는 경쟁과 분산 마감 경쟁은 이 제어의 범위 밖입니다.

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
API/Batch 스키마 검증도 V13까지 적용되었는지 확인합니다.

실제 PostgreSQL은 이 테스트 전용으로 만든 disposable PostgreSQL 16 DB에 연결합니다.
예시의 포트는 실행자가 생성한 테스트 인스턴스의 포트로 바꿉니다. 사용자 `postgres`, 비밀번호
없는 테스트 trust 인증을 사용하며 운영 설정 파일이나 자격증명을 읽지 않습니다.
각 테스트 DB fixture는 새 `posting_<random>` 스키마를 만들므로 기존 업무 스키마를 수정하지 않습니다.

```bash
JOURNAL_POSTING_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:43867/posting760 \
  ./gradlew :journal-ledger:core:test \
  --tests '*PostingConcurrencyIntegrationTest' --tests '*PostingIdentityMigrationTest' \
  --rerun-tasks --offline --max-workers=1
```

기대 결과는 14개 테스트 통과(동시성/트랜잭션 9개, migration 5개), 실패/오류/skip 0입니다.
PostgreSQL 실행에서는 두 번째 연결의 `pg_stat_activity.wait_event_type = 'Lock'`도 직접
확인합니다. 이것은 독립 연결과 실제 커밋/롤백 검증이며 운영 부하, 프로세스 강제 종료,
분산 장애 주입, 운영 데이터 정합성이나 서로 다른 전표의 잔액 경쟁을 검증하지는 않습니다.
실제 실행한 DB/명령/결과는 Issue 작업 기록과 PR 검증 근거를 기준으로 확인합니다.
