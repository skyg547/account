# CL12 / Issue #888: 연차 원천 PostgreSQL 검증 기록

2026-10-09에 **임시 PostgreSQL 16 Alpine**에서 합성 데이터로 실행했습니다. 운영 데이터나 운영
설정은 사용하지 않았습니다. 이 검증은 100M행 처리 능력의 증거가 아닙니다.

## 재현 데이터와 실행 명령

Journal V11의 연차 조회 관련 컬럼 타입을 사용한 두 테이블에 `generate_series(1,250000)`으로
2026-06-30 `POSTED` 전표 250,000건을 만들었습니다. 전표마다 수익 대변 1.00과 자산 차변
1.00을 넣어 상세 500,000건을 만들었습니다. `journal_entries(status, accounting_date)`와
`journal_details(journal_entry_id)` 인덱스를 만든 뒤 두 테이블을 `ANALYZE`했습니다.
처음 측정한 `SELECT COUNT(*)`는 전표 250,000건, 상세 500,000건이었습니다.

재현용 핵심 fixture:

```sql
INSERT INTO journal_entries
SELECT i, DATE '2026-06-30', 'KRW', DATE '2026-06-30',
       'SRC-' || i, 'POSTED', 'NORMAL', NULL, NULL, 'Source ' || i
FROM generate_series(1,250000) i;
INSERT INTO journal_details
SELECT i*2-1, i, 'CREDIT', '41000', 1, 1, NULL, NULL, NULL
FROM generate_series(1,250000) i;
INSERT INTO journal_details
SELECT i*2, i, 'DEBIT', '10000', 1, 1, NULL, NULL, NULL
FROM generate_series(1,250000) i;
CREATE INDEX idx_journal_entries_status_date ON journal_entries(status, accounting_date);
CREATE INDEX idx_journal_details_entry ON journal_details(journal_entry_id);
ANALYZE journal_entries;
ANALYZE journal_details;
```

두 조회에 `EXPLAIN (ANALYZE, BUFFERS)`를 실행했습니다. SQL 원문은
`JdbcAnnualJournalReadAdapter.STREAM_SQL`과 `CONTROL_SQL`이며 `?`에는
`2026-01-01`, `2026-12-31`을 넣었습니다. 아래는 실제 반환된 계획의 주요 노드와 시간입니다. 아래 출력은 헤더 구분선만 생략했습니다.

```text
Incremental Sort  (cost=34350.21..241796.44 rows=497450 width=1141) (actual time=120.159..1275.442 rows=500000 loops=1)
  Sort Key: (((((length((e.id)::text))::text || ':'::text) || (e.id)::text))::text) COLLATE "C", (((((length((d.id)::text))::text || ':'::text) || (d.id)::text))::text) COLLATE "C"
  Presorted Key: (((((length((e.id)::text))::text || ':'::text) || (e.id)::text))::text)
  Full-sort Groups: 15625  Sort Method: quicksort  Average Memory: 29kB  Peak Memory: 29kB
  Buffers: shared hit=1251958 read=1233, temp read=2638 written=2644
  ->  Nested Loop Left Join  (cost=34349.42..228116.56 rows=497450 width=1141) (actual time=120.001..958.571 rows=500000 loops=1)
        Buffers: shared hit=1251958 read=1233, temp read=2638 written=2644
        ->  Gather Merge  (cost=34349.00..63317.12 rows=248725 width=393) (actual time=119.957..199.618 rows=250000 loops=1)
              Workers Planned: 2
              Workers Launched: 2
              Buffers: shared hit=3191, temp read=2638 written=2644
              ->  Sort  (cost=33348.98..33608.06 rows=103635 width=393) (actual time=100.092..116.227 rows=83333 loops=3)
                    Sort Key: (((((length((e.id)::text))::text || ':'::text) || (e.id)::text))::text) COLLATE "C"
                    Sort Method: external merge  Disk: 8080kB
                    Buffers: shared hit=3191, temp read=2638 written=2644
                    Worker 0:  Sort Method: external merge  Disk: 6480kB
                    Worker 1:  Sort Method: external merge  Disk: 6544kB
                    ->  Parallel Seq Scan on journal_entries e  (cost=0.00..5941.58 rows=103635 width=393) (actual time=12.048..60.287 rows=83333 loops=3)
                          Filter: ((accounting_date >= '2026-01-01'::date) AND (accounting_date <= '2026-12-31'::date) AND ((((status)::text = 'POSTED'::text) AND ((COALESCE(lineage_source_type, ''::character varying))::text <> 'ANNUAL_CLOSING'::text) AND ((slip_no)::text !~~ 'ACL%'::text)) OR ((lineage_source_type)::text = 'ANNUAL_CLOSING'::text) OR ((slip_no)::text ~~ 'ACL%'::text)))
                          Buffers: shared hit=3077
        ->  Index Scan using idx_journal_details_entry on journal_details d  (cost=0.42..0.55 rows=2 width=692) (actual time=0.001..0.002 rows=2 loops=250000)
              Index Cond: (journal_entry_id = e.id)
              Buffers: shared hit=1248767 read=1233
Planning:
  Buffers: shared hit=328 read=5
Planning Time: 3.085 ms
JIT:
  Functions: 18
  Options: Inlining false, Optimization false, Expressions true, Deforming true
  Timing: Generation 8.234 ms, Inlining 0.000 ms, Optimization 3.324 ms, Emission 32.761 ms, Total 44.320 ms
Execution Time: 2154.039 ms

Aggregate  (cost=55923.20..55923.21 rows=1 width=88) (actual time=422.463..422.464 rows=1 loops=1)
  Buffers: shared hit=504996
  ->  Merge Left Join  (cost=1.60..47217.82 rows=497450 width=27) (actual time=0.032..303.393 rows=500000 loops=1)
        Merge Cond: (e.id = d.journal_entry_id)
        Buffers: shared hit=504996
        ->  Index Scan using journal_entries_pkey on journal_entries e  (cost=0.42..12703.42 rows=248725 width=8) (actual time=0.017..74.236 rows=250000 loops=1)
              Filter: ((accounting_date >= '2026-01-01'::date) AND (accounting_date <= '2026-12-31'::date) AND ((COALESCE(lineage_source_type, ''::character varying))::text <> 'ANNUAL_CLOSING'::text) AND ((slip_no)::text !~~ 'ACL%'::text) AND ((status)::text = 'POSTED'::text))
              Buffers: shared hit=3762
        ->  Index Scan using idx_journal_details_entry on journal_details d  (cost=0.42..27771.00 rows=500000 width=27) (actual time=0.013..127.593 rows=500000 loops=1)
              Buffers: shared hit=501234
Planning:
  Buffers: shared hit=41
Planning Time: 0.435 ms
Execution Time: 422.530 ms
```

Cursor SQL은 V2 lineage와 정확히 같은 정렬을 유지하기 위해 전표 ID의 자릿수 문자열로
정렬합니다. 이 계획에서는 외부 병합 정렬이 발생했습니다. 현재 60초 statement timeout으로
100M 데이터의 완료를 보장할 수 없습니다. 실제 운영 통계·디스크·메모리·동시성에서
`EXPLAIN (ANALYZE, BUFFERS)`와 부하 시간을 다시 측정한 뒤 timeout과 인덱스/정렬 전략을
승인해야 합니다.

## 시간 제한, cutoff와 재시작

`SET statement_timeout='1ms'` 후 연도 내 전표/상세 JOIN `COUNT(*)`를 실행하면 PostgreSQL이
`ERROR: canceling statement due to statement timeout`으로 중단했습니다. 다음 독립 실행의
전기 상세 수/수익 대변 합계는 정상적으로 `500004|250002.00`을 반환했습니다. 이 값은
계획 측정 후 cutoff 실험으로 전표 2건·상세 4건을 추가한 결과입니다.

실제 JDBC 어댑터 통합 테스트 `JdbcAnnualJournalReadAdapterPostgresTest`는 별도 localhost
임시 PostgreSQL에서 실행했습니다. 첫 cursor 중 다른 연결이 전표 1건을 커밋했지만 첫 pass,
digest pass, 공급자 합계가 모두 최초 2건/상세 4건/최대 ID 2로 일치했습니다. 새 `scan`에서는
3건/최대 ID 3을 읽었습니다. 10자리 ID를 추가하면 실제 PostgreSQL cursor는 V2 canonical 순서대로 10자리 ID를 먼저 반환합니다. 실행 명령:

```sh
CLOSING_ANNUAL_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:15438/postgres \
  ./gradlew :closing:core:test --tests '*JdbcAnnualJournalReadAdapterPostgresTest' --offline
```

실행 직후 복사한 [JUnit XML](annual-close-postgres-junit.xml)은 로컬 호스트명만
일반 값으로 치환했으며, 1건 실행, skip/failure/error 각 0건, 2.093초를 기록합니다.

`AnnualClosingServiceTest`는 같은 V2 source의 초안 재실행, 이전 `POSTED` 결산 후 조정
delta, 재전기 후 무처리, legacy `POSTED` lineage, 공급자 반환 순서 변경을 검증합니다.
`AnnualClosingStreamingRegressionTest`는 균형 잡힌 전표 20,000건을 생성형 공급자로
순회하며, 공급자 호출 1회, 기준일·계정별 Master 조회 각 1회, 공급자 합계 불일치 및
분류 키 초과 시 초안 미생성을 검증합니다. JVM heap 최대 사용량 수치, 실제 원격 HTTP
latency, 100M행, 운영 PostgreSQL, 분산 장애 주입은 측정하지 않았습니다.
