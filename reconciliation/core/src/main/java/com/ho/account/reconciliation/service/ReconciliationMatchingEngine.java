package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.domain.ReconciliationItem;
import com.ho.account.reconciliation.service.ItemLevelMatcher.ItemMatchGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * [헥사고날 아키텍처 - 도메인 서비스 / 대사 매칭 엔진 엔트리포인트]
 * 항목 수준(Item-Level) 건별 N:M 매칭 알고리즘 오케스트레이터.
 * 
 * 🐣 [초보자를 위한 개념 설명: ReconciliationMatchingEngine의 역할]
 * 1. 역할과 책임:
 *    - `ItemLevelMatcher` 알고리즘을 활용하여 원천(Source) 항목과 대상(Target) 항목 집합 간의
 *      1:1, 1:N, N:1, N:M 자동 대사 매칭을 총괄합니다.
 *    - 대사 매칭 실행 결과를 집계(총 건수, 총 금액, 매칭 완료 건수/금액, 불일치 건수/금액)하고
 *      감사 추적(Audit Trail)용 매칭 그룹 및 불일치(Discrepancy) 그룹 리스트를 포장하여 서비스 레이어에 전달합니다.
 * 
 * 2. 금융 감사 및 추적성(Audit Trail & Traceability):
 *    - 대사 실행 후 단순히 "성공/실패" 여부만 남기는 것이 아니라,
 *      어떤 원천 항목들이 어떤 대상 항목들과 N:M 매칭되었는지(`matchedGroups`),
 *      어떤 항목들이 차액이나 누락으로 불일치 처리되었는지(`discrepancyGroups`)를 명확한 구조체로 반환합니다.
 */
@Component
public class ReconciliationMatchingEngine {

    private final ItemLevelMatcher itemLevelMatcher;

    @Autowired
    public ReconciliationMatchingEngine(ItemLevelMatcher itemLevelMatcher) {
        this.itemLevelMatcher = Objects.requireNonNull(itemLevelMatcher, "itemLevelMatcher must not be null");
    }

    public record ExecutionResult(
            long totalSourceCount,
            BigDecimal totalSourceAmount,
            long totalTargetCount,
            BigDecimal totalTargetAmount,
            long matchedItemsCount,
            BigDecimal matchedAmount,
            long unmatchedItemsCount,
            BigDecimal unmatchedAmount,
            List<ItemMatchGroup> matchedGroups,
            List<ItemMatchGroup> discrepancyGroups
    ) {
        public ExecutionResult {
            totalSourceAmount = totalSourceAmount != null ? totalSourceAmount : BigDecimal.ZERO;
            totalTargetAmount = totalTargetAmount != null ? totalTargetAmount : BigDecimal.ZERO;
            matchedAmount = matchedAmount != null ? matchedAmount : BigDecimal.ZERO;
            unmatchedAmount = unmatchedAmount != null ? unmatchedAmount : BigDecimal.ZERO;
            matchedGroups = matchedGroups != null ? List.copyOf(matchedGroups) : List.of();
            discrepancyGroups = discrepancyGroups != null ? List.copyOf(discrepancyGroups) : List.of();
        }
    }

    public ExecutionResult matchItems(List<ReconciliationItem> sourceItems,
                                       List<ReconciliationItem> targetItems,
                                       BigDecimal amountTolerance) {
        return matchItems(sourceItems, targetItems, amountTolerance, 4);
    }

    public ExecutionResult matchItems(List<ReconciliationItem> sourceItems,
                                       List<ReconciliationItem> targetItems,
                                       BigDecimal amountTolerance,
                                       int maxSubsetSize) {
        List<ReconciliationItem> sources = sourceItems != null ? sourceItems : List.of();
        List<ReconciliationItem> targets = targetItems != null ? targetItems : List.of();

        BigDecimal totalSourceAmount = sources.stream()
                .map(ReconciliationItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTargetAmount = targets.stream()
                .map(ReconciliationItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ItemMatchGroup> allGroups = itemLevelMatcher.match(sources, targets, amountTolerance, maxSubsetSize);

        List<ItemMatchGroup> matchedGroups = new ArrayList<>();
        List<ItemMatchGroup> discrepancyGroups = new ArrayList<>();

        long matchedSourceCount = 0;
        long matchedTargetCount = 0;
        BigDecimal matchedSourceAmount = BigDecimal.ZERO;
        BigDecimal matchedTargetAmount = BigDecimal.ZERO;

        for (ItemMatchGroup group : allGroups) {
            if (group.isMatched()) {
                matchedGroups.add(group);
                matchedSourceCount += group.sourceItems().size();
                matchedTargetCount += group.targetItems().size();
                matchedSourceAmount = matchedSourceAmount.add(group.sourceTotalAmount());
                matchedTargetAmount = matchedTargetAmount.add(group.targetTotalAmount());
            } else {
                discrepancyGroups.add(group);
            }
        }

        long matchedItemsCount = Math.min(matchedSourceCount, matchedTargetCount);
        if (matchedSourceCount == 0 && matchedTargetCount == 0 && !matchedGroups.isEmpty()) {
            matchedItemsCount = matchedGroups.size();
        }

        BigDecimal matchedAmount = matchedSourceAmount.min(matchedTargetAmount);

        long unmatchedSourceCount = sources.size() - matchedSourceCount;
        long unmatchedTargetCount = targets.size() - matchedTargetCount;
        long unmatchedItemsCount = Math.max(0, unmatchedSourceCount) + Math.max(0, unmatchedTargetCount);

        // If there are discrepancy groups with amount mismatch, ensure count reflects unmatched
        if (unmatchedItemsCount == 0 && !discrepancyGroups.isEmpty()) {
            unmatchedItemsCount = discrepancyGroups.size();
        }

        BigDecimal unmatchedAmount = discrepancyGroups.stream()
                .map(ItemMatchGroup::differenceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ExecutionResult(
                sources.size(),
                totalSourceAmount,
                targets.size(),
                totalTargetAmount,
                matchedItemsCount,
                matchedAmount,
                unmatchedItemsCount,
                unmatchedAmount,
                matchedGroups,
                discrepancyGroups
        );
    }
}
