package com.ho.account.ecl.batch.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcOperations;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * [배치] 컬럼 및 기준일(baseDate) 연동 범위 파티셔너 (Column & BaseDate Range Partitioner)
 *
 * 💡 [비즈니스 가이드]
 * 대용량 데이터를 파티셔닝할 때, 기준일(baseDate) 조건 없이 ID 최소/최대값만 조회하면
 * 상이한 기준일 간의 ID 겹침 현상으로 인해 파티션 범위 불일치가 발생할 수 있습니다.
 * 이 클래스는 테이블, 대상 컬럼 및 선택적 기준일 컬럼/기준일 값을 수용하여
 * 해당 기준일에 한정된 정합성 높은 파티션 범위를 산출합니다.
 */
@Slf4j
public class ColumnRangePartitioner implements Partitioner {

    private final JdbcOperations jdbcTemplate;
    private final String table;
    private final String column;
    private final String dateColumn;
    private final LocalDate baseDate;

    public ColumnRangePartitioner(JdbcOperations jdbcTemplate, String table, String column) {
        this(jdbcTemplate, table, column, null, null);
    }

    public ColumnRangePartitioner(JdbcOperations jdbcTemplate, String table, String column, String dateColumn, LocalDate baseDate) {
        this.jdbcTemplate = jdbcTemplate;
        this.table = table;
        this.column = column;
        this.dateColumn = dateColumn;
        this.baseDate = baseDate;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        String sql;
        Map<String, Object> bounds;

        if (dateColumn != null && !dateColumn.isBlank() && baseDate != null) {
            sql = String.format("SELECT MIN(%s) AS min_value, MAX(%s) AS max_value FROM %s WHERE %s = ?", column, column, table, dateColumn);
            bounds = jdbcTemplate.queryForMap(sql, java.sql.Date.valueOf(baseDate));
        } else {
            sql = String.format("SELECT MIN(%s) AS min_value, MAX(%s) AS max_value FROM %s", column, column, table);
            bounds = jdbcTemplate.queryForMap(sql);
        }

        Long min = bounds.get("min_value") == null ? null : ((Number) bounds.get("min_value")).longValue();
        Long max = bounds.get("max_value") == null ? null : ((Number) bounds.get("max_value")).longValue();

        if (min == null || max == null) {
            log.warn("⚠️ [Partitioner] 파티셔닝할 데이터 범위를 찾을 수 없습니다. (Table: {}, Date: {}, Min: {}, Max: {})", table, baseDate, min, max);
            ExecutionContext context = new ExecutionContext();
            context.putLong("minValue", 0L);
            context.putLong("maxValue", -1L);
            return Map.of("partition0", context);
        }

        if (min.equals(max)) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minValue", min);
            context.putLong("maxValue", max);
            return Map.of("partition0", context);
        }

        long targetSize = (max - min) / gridSize + 1;
        Map<String, ExecutionContext> result = new HashMap<>();

        long start = min;
        long end = start + targetSize - 1;
        int number = 0;

        while (start <= max) {
            if (end >= max) {
                end = max;
            }

            ExecutionContext context = new ExecutionContext();
            context.putLong("minValue", start);
            context.putLong("maxValue", end);
            result.put("partition" + number, context);

            log.debug("📦 [Partitioner] 파티션[{}] 할당: {} ~ {}", number, start, end);

            start += targetSize;
            end += targetSize;
            number++;
        }

        log.info("✅ [Partitioner] 총 {}개의 파티션 분할 완료 (Table: {}, Date: {}, TargetSize: {})", result.size(), table, baseDate, targetSize);
        return result;
    }
}
