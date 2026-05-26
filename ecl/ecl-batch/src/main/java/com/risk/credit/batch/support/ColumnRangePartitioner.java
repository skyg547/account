package com.risk.credit.batch.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcOperations;

import java.util.HashMap;
import java.util.Map;

/**
 * [배치] 컬럼 범위 파티셔너 (Column Range Partitioner)
 *
 * 💡 [비즈니스 가이드]
 * 억 단위의 대용량 데이터를 처리할 때, 한 줄로 서서 일을 하면 평생 걸려도 못 끝낼 수 있습니다.
 * 이 클래스는 일을 공평하게 나누어 주는 '작업 분배관'입니다.
 * 전체 계좌 ID의 최소값(Min)과 최대값(Max)을 기반으로, 지정된 숫자(gridSize)만큼 범위를 쪼개 줍니다.
 */
@Slf4j
@RequiredArgsConstructor
public class ColumnRangePartitioner implements Partitioner {

    private final JdbcOperations jdbcTemplate;
    private final String table;
    private final String column;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        String sql = String.format("SELECT MIN(%s) AS min_value, MAX(%s) AS max_value FROM %s", column, column, table);
        Map<String, Object> bounds = jdbcTemplate.queryForMap(sql);

        Long min = bounds.get("min_value") == null ? null : ((Number) bounds.get("min_value")).longValue();
        Long max = bounds.get("max_value") == null ? null : ((Number) bounds.get("max_value")).longValue();

        if (min == null || max == null) {
            log.warn("⚠️ [Partitioner] 파티셔닝할 데이터 범위를 찾을 수 없습니다. (Min: {}, Max: {})", min, max);
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

        log.info("✅ [Partitioner] 총 {}개의 파티션 분할 완료 (TargetSize: {})", result.size(), targetSize);
        return result;
    }
}
