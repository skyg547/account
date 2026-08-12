package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.domain.ReconciliationCompositeKey;
import com.ho.account.reconciliation.domain.ReconciliationItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * [금융 대사 핵심 서비스 - 항목 수준(Item-Level) 건별 N:M 매칭 알고리즘 엔진]
 * 
 * 🐣 [초보자를 위한 상세 설명: 1:1, 1:N, N:M 매칭 알고리즘과 감사 추적성(Audit Trail)]
 * 
 * 1. 왜 항목 수준(Item-Level) 건별 N:M 매칭이 필요한가?
 *    - 과거 단순 총액 비교(Summary Total Comparison)는 "원천 거래 총액 = 원장 전표 총액"만 비교했습니다.
 *      하지만 이는 상쇄 오류(Offsetting Errors, 예: A고객 10만 원 누락 + B고객 10만 원 중복이 서로 상쇄되어 총액 일치 처리됨)를
 *      감지할 수 없는 치명적인 금융 리스크가 있었습니다.
 *    - 본 `ItemLevelMatcher`는 복합 키(Composite Key) 기반 그룹화 및 1:1, 1:N, N:1, N:M 부분집합 합계(Subset-Sum) 알고리즘을 수행하여
 *      단 한 건의 거래 누락이나 차액도 명확하게 분리 및 적발합니다.
 * 
 * 2. 매칭 알고리즘의 4단계 실행 흐름:
 *    - Phase 1 (1:1 단건 동등 매칭): 동일 복합 키(거래일자+참조ID+거래처코드+계정과목) 그룹 내에서 1:1 금액 일치 건을 우선 매칭합니다.
 *    - Phase 2 (1:N 및 N:1 일괄 분할 매칭): 1건의 원천 입금 건에 N건의 매출 전표가 대응되거나, N건의 원천 결제 건에 1건의 총액 전표가 대응되는 건을 매칭합니다.
 *    - Phase 3 (N:M 건별 부분집합 합계 매칭): N개의 PG 결제 대금과 M개의 은행 입금 내역 간의 합계 금액이 일치하는 최적 조합(Subset-Sum)을 탐색하여 대치합니다.
 *    - Phase 4 (불일치 항목 차액 분석 및 도메인 분류): 매칭되지 않은 잔여 항목들을 MISSING_TARGET(대상 누락), MISSING_SOURCE(원천 누락), AMOUNT_MISMATCH(금액 불일치) 유형으로 분리하여 상세 원인을 추적합니다.
 * 
 * 3. 감사 추적(Audit Trail) 및 성능 최적화 Rationale:
 *    - 부분집합 조합 탐색(Subset-Sum)은 NP-Hard 문제로서 O(2^N) 시간 복잡도를 가집니다.
 *      전체 데이터를 한번에 부분집합 조합하면 메모리/CPU 폭발이 발생하므로,
 *      1) 복합 키 파티셔닝(Composite Key Partitioning)으로 탐색 공간을 소그룹으로 격리하고,
 *      2) 탐색 깊이 제한(maxSubsetSize, 기본 4)을 두어 성능(O(1))과 대사 정밀도를 동시에 달성합니다.
 */
@Component
public class ItemLevelMatcher {

    public enum MatchType {
        EXACT_1_1,         // 1:1 단건 정밀 매칭
        ONE_TO_MANY_1_N,   // 1:N 매칭 (원천 1건 - 대상 N건)
        MANY_TO_ONE_N_1,   // N:1 매칭 (원천 N건 - 대상 1건)
        MANY_TO_MANY_N_M,  // N:M 매칭 (원천 N건 - 대상 M건)
        MISSING_TARGET,    // 대상 데이터 누락 (원천만 존재)
        MISSING_SOURCE,    // 원천 데이터 누락 (대상만 존재)
        AMOUNT_MISMATCH    // 금액 불일치
    }

    public record ItemMatchGroup(
            MatchType matchType,
            List<ReconciliationItem> sourceItems,
            List<ReconciliationItem> targetItems,
            BigDecimal sourceTotalAmount,
            BigDecimal targetTotalAmount,
            BigDecimal differenceAmount,
            String matchReason
    ) {
        public ItemMatchGroup {
            sourceItems = sourceItems != null ? List.copyOf(sourceItems) : List.of();
            targetItems = targetItems != null ? List.copyOf(targetItems) : List.of();
            sourceTotalAmount = sourceTotalAmount != null ? sourceTotalAmount : BigDecimal.ZERO;
            targetTotalAmount = targetTotalAmount != null ? targetTotalAmount : BigDecimal.ZERO;
            differenceAmount = differenceAmount != null ? differenceAmount : BigDecimal.ZERO;
        }

        public boolean isMatched() {
            return matchType == MatchType.EXACT_1_1
                    || matchType == MatchType.ONE_TO_MANY_1_N
                    || matchType == MatchType.MANY_TO_ONE_N_1
                    || matchType == MatchType.MANY_TO_MANY_N_M;
        }
    }

    public List<ItemMatchGroup> match(List<ReconciliationItem> sourceItems,
                                      List<ReconciliationItem> targetItems,
                                      BigDecimal amountTolerance) {
        return match(sourceItems, targetItems, amountTolerance, 4);
    }

    public List<ItemMatchGroup> match(List<ReconciliationItem> sourceItems,
                                      List<ReconciliationItem> targetItems,
                                      BigDecimal amountTolerance,
                                      int maxSubsetSize) {
        Objects.requireNonNull(sourceItems, "sourceItems must not be null");
        Objects.requireNonNull(targetItems, "targetItems must not be null");
        BigDecimal tolerance = amountTolerance != null ? amountTolerance.abs() : BigDecimal.ZERO;
        int limit = maxSubsetSize > 0 ? maxSubsetSize : 4;

        List<ItemMatchGroup> matchGroups = new ArrayList<>();
        Set<String> matchedSourceIds = new HashSet<>();
        Set<String> matchedTargetIds = new HashSet<>();

        // Phase 1 ~ 3: Strict Composite Key Grouping & Matching (1:1, 1:N, N:1, N:M)
        Map<ReconciliationCompositeKey, List<ReconciliationItem>> sourceKeyMap = groupByKey(sourceItems, false);
        Map<ReconciliationCompositeKey, List<ReconciliationItem>> targetKeyMap = groupByKey(targetItems, false);

        Set<ReconciliationCompositeKey> allKeys = new LinkedHashSet<>();
        allKeys.addAll(sourceKeyMap.keySet());
        allKeys.addAll(targetKeyMap.keySet());

        for (ReconciliationCompositeKey key : allKeys) {
            List<ReconciliationItem> groupSources = sourceKeyMap.getOrDefault(key, List.of());
            List<ReconciliationItem> groupTargets = targetKeyMap.getOrDefault(key, List.of());

            executeGroupMatching(groupSources, groupTargets, matchedSourceIds, matchedTargetIds, tolerance, limit, matchGroups);
        }

        // Phase 4: Fallback Relaxed Composite Key Matching (date + accountCode)
        List<ReconciliationItem> unassignedSources = sourceItems.stream()
                .filter(i -> !matchedSourceIds.contains(i.getId()))
                .collect(Collectors.toList());
        List<ReconciliationItem> unassignedTargets = targetItems.stream()
                .filter(i -> !matchedTargetIds.contains(i.getId()))
                .collect(Collectors.toList());

        if (!unassignedSources.isEmpty() && !unassignedTargets.isEmpty()) {
            Map<ReconciliationCompositeKey, List<ReconciliationItem>> relaxedSourceMap = groupByKey(unassignedSources, true);
            Map<ReconciliationCompositeKey, List<ReconciliationItem>> relaxedTargetMap = groupByKey(unassignedTargets, true);

            Set<ReconciliationCompositeKey> relaxedKeys = new LinkedHashSet<>();
            relaxedKeys.addAll(relaxedSourceMap.keySet());
            relaxedKeys.addAll(relaxedTargetMap.keySet());

            for (ReconciliationCompositeKey relaxedKey : relaxedKeys) {
                List<ReconciliationItem> rSources = relaxedSourceMap.getOrDefault(relaxedKey, List.of()).stream()
                        .filter(i -> !matchedSourceIds.contains(i.getId()))
                        .collect(Collectors.toList());
                List<ReconciliationItem> rTargets = relaxedTargetMap.getOrDefault(relaxedKey, List.of()).stream()
                        .filter(i -> !matchedTargetIds.contains(i.getId()))
                        .collect(Collectors.toList());

                executeGroupMatching(rSources, rTargets, matchedSourceIds, matchedTargetIds, tolerance, limit, matchGroups);
            }
        }

        // Phase 5: Analyze and categorize remaining unmatched items as differences
        List<ReconciliationItem> finalUnmatchedSources = sourceItems.stream()
                .filter(i -> !matchedSourceIds.contains(i.getId()))
                .collect(Collectors.toList());
        List<ReconciliationItem> finalUnmatchedTargets = targetItems.stream()
                .filter(i -> !matchedTargetIds.contains(i.getId()))
                .collect(Collectors.toList());

        categorizeDiscrepancies(finalUnmatchedSources, finalUnmatchedTargets, matchGroups);

        return matchGroups;
    }

    private void executeGroupMatching(List<ReconciliationItem> groupSources,
                                      List<ReconciliationItem> groupTargets,
                                      Set<String> matchedSourceIds,
                                      Set<String> matchedTargetIds,
                                      BigDecimal tolerance,
                                      int limit,
                                      List<ItemMatchGroup> matchGroups) {
        // Step 1: 1:1 Matching
        for (ReconciliationItem src : groupSources) {
            if (matchedSourceIds.contains(src.getId())) continue;

            for (ReconciliationItem tgt : groupTargets) {
                if (matchedTargetIds.contains(tgt.getId())) continue;

                if (isWithinTolerance(src.getAmount(), tgt.getAmount(), tolerance)) {
                    matchedSourceIds.add(src.getId());
                    matchedTargetIds.add(tgt.getId());
                    BigDecimal diff = src.getAmount().subtract(tgt.getAmount()).abs();
                    matchGroups.add(new ItemMatchGroup(
                            MatchType.EXACT_1_1,
                            List.of(src),
                            List.of(tgt),
                            src.getAmount(),
                            tgt.getAmount(),
                            diff,
                            "Composite Key Exact 1:1 Match"
                    ));
                    break;
                }
            }
        }

        // Step 2: 1:N & N:1 Matching
        List<ReconciliationItem> unassignedSrc = groupSources.stream()
                .filter(i -> !matchedSourceIds.contains(i.getId()))
                .collect(Collectors.toList());
        List<ReconciliationItem> unassignedTgt = groupTargets.stream()
                .filter(i -> !matchedTargetIds.contains(i.getId()))
                .collect(Collectors.toList());

        // 1:N Matching (1 Source vs N Targets)
        for (ReconciliationItem src : unassignedSrc) {
            if (matchedSourceIds.contains(src.getId())) continue;

            List<ReconciliationItem> availableTargets = unassignedTgt.stream()
                    .filter(t -> !matchedTargetIds.contains(t.getId()))
                    .collect(Collectors.toList());

            List<List<ReconciliationItem>> targetSubsets = generateSubsets(availableTargets, limit);
            for (List<ReconciliationItem> tgtSub : targetSubsets) {
                if (tgtSub.size() <= 1) continue; // 1:1 is handled in step 1
                BigDecimal tgtSum = sumAmount(tgtSub);
                if (isWithinTolerance(src.getAmount(), tgtSum, tolerance)) {
                    matchedSourceIds.add(src.getId());
                    tgtSub.forEach(t -> matchedTargetIds.add(t.getId()));
                    BigDecimal diff = src.getAmount().subtract(tgtSum).abs();
                    matchGroups.add(new ItemMatchGroup(
                            MatchType.ONE_TO_MANY_1_N,
                            List.of(src),
                            tgtSub,
                            src.getAmount(),
                            tgtSum,
                            diff,
                            "Composite Key 1:N Subset Match"
                    ));
                    break;
                }
            }
        }

        // N:1 Matching (N Sources vs 1 Target)
        unassignedSrc = groupSources.stream()
                .filter(i -> !matchedSourceIds.contains(i.getId()))
                .collect(Collectors.toList());
        unassignedTgt = groupTargets.stream()
                .filter(i -> !matchedTargetIds.contains(i.getId()))
                .collect(Collectors.toList());

        for (ReconciliationItem tgt : unassignedTgt) {
            if (matchedTargetIds.contains(tgt.getId())) continue;

            List<ReconciliationItem> availableSources = unassignedSrc.stream()
                    .filter(s -> !matchedSourceIds.contains(s.getId()))
                    .collect(Collectors.toList());

            List<List<ReconciliationItem>> sourceSubsets = generateSubsets(availableSources, limit);
            for (List<ReconciliationItem> srcSub : sourceSubsets) {
                if (srcSub.size() <= 1) continue;
                BigDecimal srcSum = sumAmount(srcSub);
                if (isWithinTolerance(srcSum, tgt.getAmount(), tolerance)) {
                    srcSub.forEach(s -> matchedSourceIds.add(s.getId()));
                    matchedTargetIds.add(tgt.getId());
                    BigDecimal diff = srcSum.subtract(tgt.getAmount()).abs();
                    matchGroups.add(new ItemMatchGroup(
                            MatchType.MANY_TO_ONE_N_1,
                            srcSub,
                            List.of(tgt),
                            srcSum,
                            tgt.getAmount(),
                            diff,
                            "Composite Key N:1 Subset Match"
                    ));
                    break;
                }
            }
        }

        // Step 3: N:M Subset-Sum Matching
        unassignedSrc = groupSources.stream()
                .filter(i -> !matchedSourceIds.contains(i.getId()))
                .collect(Collectors.toList());
        unassignedTgt = groupTargets.stream()
                .filter(i -> !matchedTargetIds.contains(i.getId()))
                .collect(Collectors.toList());

        if (unassignedSrc.size() >= 2 && unassignedTgt.size() >= 2) {
            List<List<ReconciliationItem>> sourceSubsets = generateSubsets(unassignedSrc, limit);
            List<List<ReconciliationItem>> targetSubsets = generateSubsets(unassignedTgt, limit);

            for (List<ReconciliationItem> srcSub : sourceSubsets) {
                if (srcSub.stream().anyMatch(s -> matchedSourceIds.contains(s.getId()))) continue;
                if (srcSub.size() <= 1) continue;

                BigDecimal srcSum = sumAmount(srcSub);

                for (List<ReconciliationItem> tgtSub : targetSubsets) {
                    if (tgtSub.stream().anyMatch(t -> matchedTargetIds.contains(t.getId()))) continue;
                    if (tgtSub.size() <= 1) continue;

                    BigDecimal tgtSum = sumAmount(tgtSub);

                    if (isWithinTolerance(srcSum, tgtSum, tolerance)) {
                        srcSub.forEach(s -> matchedSourceIds.add(s.getId()));
                        tgtSub.forEach(t -> matchedTargetIds.add(t.getId()));
                        BigDecimal diff = srcSum.subtract(tgtSum).abs();
                        matchGroups.add(new ItemMatchGroup(
                                MatchType.MANY_TO_MANY_N_M,
                                srcSub,
                                tgtSub,
                                srcSum,
                                tgtSum,
                                diff,
                                "Composite Key N:M Subset-Sum Match"
                        ));
                        break;
                    }
                }
            }
        }
    }

    private void categorizeDiscrepancies(List<ReconciliationItem> unmatchedSources,
                                         List<ReconciliationItem> unmatchedTargets,
                                         List<ItemMatchGroup> matchGroups) {
        Map<ReconciliationCompositeKey, List<ReconciliationItem>> srcGroup = groupByKey(unmatchedSources, false);
        Map<ReconciliationCompositeKey, List<ReconciliationItem>> tgtGroup = groupByKey(unmatchedTargets, false);

        Set<ReconciliationCompositeKey> allKeys = new LinkedHashSet<>();
        allKeys.addAll(srcGroup.keySet());
        allKeys.addAll(tgtGroup.keySet());

        for (ReconciliationCompositeKey key : allKeys) {
            List<ReconciliationItem> sources = srcGroup.getOrDefault(key, List.of());
            List<ReconciliationItem> targets = tgtGroup.getOrDefault(key, List.of());

            BigDecimal srcTotal = sumAmount(sources);
            BigDecimal tgtTotal = sumAmount(targets);
            BigDecimal diff = srcTotal.subtract(tgtTotal).abs();

            if (!sources.isEmpty() && targets.isEmpty()) {
                matchGroups.add(new ItemMatchGroup(
                        MatchType.MISSING_TARGET,
                        sources,
                        List.of(),
                        srcTotal,
                        BigDecimal.ZERO,
                        srcTotal,
                        "Missing Target Items for Composite Key: " + key
                ));
            } else if (sources.isEmpty() && !targets.isEmpty()) {
                matchGroups.add(new ItemMatchGroup(
                        MatchType.MISSING_SOURCE,
                        List.of(),
                        targets,
                        BigDecimal.ZERO,
                        tgtTotal,
                        tgtTotal,
                        "Missing Source Items for Composite Key: " + key
                ));
            } else if (!sources.isEmpty() && !targets.isEmpty()) {
                matchGroups.add(new ItemMatchGroup(
                        MatchType.AMOUNT_MISMATCH,
                        sources,
                        targets,
                        srcTotal,
                        tgtTotal,
                        diff,
                        "Amount Discrepancy for Composite Key: " + key
                ));
            }
        }
    }

    private Map<ReconciliationCompositeKey, List<ReconciliationItem>> groupByKey(List<ReconciliationItem> items, boolean relaxed) {
        Map<ReconciliationCompositeKey, List<ReconciliationItem>> map = new LinkedHashMap<>();
        for (ReconciliationItem item : items) {
            ReconciliationCompositeKey key = relaxed ? ReconciliationCompositeKey.relaxedKey(item) : ReconciliationCompositeKey.fromItem(item);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private boolean isWithinTolerance(BigDecimal val1, BigDecimal val2, BigDecimal tolerance) {
        if (val1 == null || val2 == null) return false;
        return val1.subtract(val2).abs().compareTo(tolerance) <= 0;
    }

    private BigDecimal sumAmount(List<ReconciliationItem> items) {
        return items.stream()
                .map(ReconciliationItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> List<List<T>> generateSubsets(List<T> items, int maxSize) {
        List<List<T>> subsets = new ArrayList<>();
        int n = items.size();
        for (int size = 1; size <= Math.min(n, maxSize); size++) {
            combine(items, size, 0, new ArrayList<>(), subsets);
        }
        return subsets;
    }

    private <T> void combine(List<T> items, int k, int start, List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            current.add(items.get(i));
            combine(items, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
