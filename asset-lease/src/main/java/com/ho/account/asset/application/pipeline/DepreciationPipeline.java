package com.ho.account.asset.application.pipeline;

import com.ho.account.asset.domain.FixedAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h3>감가상각 파이프라인 (Depreciation Pipeline)</h3>
 * Batch Chunk 단위로 들어온 대량의 자산 데이터를 도메인 규칙에 따라 계산하고 변환합니다.
 * (Agents.md: 비즈니스 연산 로직 집중 계층)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepreciationPipeline {

    /**
     * 자산 목록을 받아 당월 감가상각비를 산출합니다.
     * @return 자산ID별 상각액 결과 맵
     */
    public Map<Long, BigDecimal> calculateBatch(List<FixedAsset> assets, LocalDate targetDate) {
        return assets.stream()
                .filter(asset -> "ACTIVE".equals(asset.getStatus()))
                .collect(Collectors.toMap(
                        FixedAsset::getId,
                        asset -> asset.depreciate(targetDate)
                ));
    }
}
