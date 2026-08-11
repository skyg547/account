package com.ho.account.reporting.core.domain.service;

import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.DisclosureNoteMartEntry;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [도메인 계산기] IFRS 주석(Disclosure Notes) 공시 데이터 집계 및 검증 코어 엔진.
 *
 * 💡 [헥사고날 아키텍처 & Pure Java POJO 원칙]
 * 이 클래스는 헥사고날 아키텍처의 핵심 도메인 영역에 속하며, 특정 프레임워크(Spring)에 의존하지 않는 Pure POJO로 작성되었습니다.
 * 어노테이션 기반 빈 스캔 대신 `ReportingDomainConfiguration`에서 수동으로 빈을 등록하여
 * 도메인 계층의 기술 독립성과 완벽한 격리를 보장합니다.
 *
 * 💡 [초보자를 위한 회계 공시 설명]
 * 상장회사 및 금융기관의 IFRS 재무제표는 숫자로 된 본문(B/S, I/S) 뒤에 세부 산출 내역과 위험 정보를 밝히는 '주석(Notes)'이 함께 제출됩니다.
 * 이 계산기는 `DisclosureNoteMart` 데이터를 주석 번호(예: "REV", "EXP", "AST", "LIA")별로 그룹화하고,
 * 당기 및 전기 금액 합계를 정밀 집계하여 IFRS 주석 공시 검증을 수행합니다.
 */
public class IfrsDisclosureNotesEngine {

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);

    /**
     * 주석 집계 결과 VO
     */
    public record NoteSummary(
            String noteNumber,
            int itemCounts,
            BigDecimal totalCurrentAmount,
            BigDecimal totalPreviousAmount,
            List<DisclosureNoteMartEntry> entries
    ) {}

    /**
     * DisclosureNoteMart 데이터를 주석 번호(noteNumber) 기준으로 집계합니다.
     *
     * @param mart 주석 공시 마트 엔티티
     * @return 주석 번호별 집계 결과 맵 (Key: noteNumber)
     */
    public Map<String, NoteSummary> summarizeDisclosureNotes(DisclosureNoteMart mart) {
        if (mart == null || mart.getEntries() == null || mart.getEntries().isEmpty()) {
            return Map.of();
        }

        Map<String, List<DisclosureNoteMartEntry>> groupedByNote = mart.getEntries().stream()
                .collect(Collectors.groupingBy(DisclosureNoteMartEntry::getNoteNumber));

        return groupedByNote.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    String noteNo = entry.getKey();
                    List<DisclosureNoteMartEntry> list = entry.getValue();

                    BigDecimal sumCurrent = list.stream()
                            .map(DisclosureNoteMartEntry::getCurrentAmount)
                            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b, MC))
                            .setScale(2, RoundingMode.HALF_UP);

                    BigDecimal sumPrevious = list.stream()
                            .map(DisclosureNoteMartEntry::getPreviousAmount)
                            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b, MC))
                            .setScale(2, RoundingMode.HALF_UP);

                    return new NoteSummary(noteNo, list.size(), sumCurrent, sumPrevious, List.copyOf(list));
                }
        ));
    }
}
