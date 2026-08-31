package com.ho.account.asset.application.pipeline;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.domain.FixedAssetDepreciationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
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
     * 자산 목록을 받아 당월 감가상각 결과를 산출합니다.
     *
     * <p>초보자 설명: 이 파이프라인은 계산 결과만 만들고 자산 엔티티를 직접 변경하지 않습니다.
     * 실제 DB 반영은 `AssetPersistencePort`의 JDBC bulk adapter가 담당합니다.</p>
     *
     * @return 자산ID별 상각액, 장부가, 상태 결과 목록
     */
    public List<FixedAssetDepreciationResult> calculateBatch(List<FixedAsset> assets, LocalDate targetDate) {
        return assets.stream()
                .filter(asset -> "ACTIVE".equals(asset.getStatus()))
                .filter(asset -> !asset.isDepreciatedForPeriod(targetDate))
                .map(asset -> asset.calculateDepreciation(targetDate))
                .filter(FixedAssetDepreciationResult::shouldPersist)
                .collect(Collectors.toList());
    }
}