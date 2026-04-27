package com.ho.account.report.service;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.report.domain.ReportLineMapping;
import com.ho.account.report.domain.ReportSnapshotDetail;
import com.ho.account.report.domain.ReportSnapshotHeader;
import com.ho.account.report.repository.ReportSnapshotHeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportSnapshotService {

    private final ReportMappingService reportMappingService;
    private final ReportSnapshotHeaderRepository headerRepository;
    private final JournalDetailRepository journalDetailRepository;

    @Autowired
    public ReportSnapshotService(ReportMappingService reportMappingService,
            ReportSnapshotHeaderRepository headerRepository,
            JournalDetailRepository journalDetailRepository) {
        this.reportMappingService = reportMappingService;
        this.headerRepository = headerRepository;
        this.journalDetailRepository = journalDetailRepository;
    }

    /**
     * ?πÏ†ï Í∏∞Ï??ºÏùò Î≥¥Í≥†?úÎ? ?ùÏÑ±?òÍ≥† ?§ÎÉÖ?∑ÏúºÎ°??Ä?•Ìï©?àÎã§.
     */
    public ReportSnapshotHeader createSnapshot(String reportType, LocalDate baseDate, String description) {
        List<ReportLineMapping> mappings = reportMappingService.getActiveMappings(reportType, baseDate);

        ReportSnapshotHeader header = new ReportSnapshotHeader();
        header.setReportType(reportType);
        header.setBaseDate(baseDate);
        header.setStatus(ReportSnapshotHeader.SnapshotStatus.DRAFT);
        header.setDescription(description);

        // Î≤ÑÏ†Ñ Í≤∞Ï†ï
        int version = headerRepository.findByReportTypeAndBaseDate(reportType, baseDate).size() + 1;
        header.setVersion(version);

        List<ReportSnapshotDetail> details = new ArrayList<>();

        for (ReportLineMapping mapping : mappings) {
            BigDecimal amount = calculateLineAmount(mapping, baseDate);

            ReportSnapshotDetail detail = new ReportSnapshotDetail();
            detail.setHeader(header);
            detail.setLineCode(mapping.getLineCode());
            detail.setAmount(amount);
            details.add(detail);
        }

        header.setDetails(details);
        return headerRepository.save(header);
    }

    /**
     * Î≥¥Í≥†???§ÎÉÖ?∑Ïùò ?πÏ†ï ?ºÏù∏ Í∏àÏï°??Íµ¨ÏÑ±?òÎäî ?ÑÌëú ?ÅÏÑ∏ ?¥Ïó≠??Ï°∞Ìöå?©Îãà??(Drill-through).
     */
    public List<JournalDetail> getContributingJournals(Long snapshotId, String lineCode) {
        ReportSnapshotHeader header = headerRepository.findById(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot not found"));

        ReportLineMapping mapping = reportMappingService.getActiveMappings(header.getReportType(), header.getBaseDate())
                .stream()
                .filter(m -> m.getLineCode().equals(lineCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Line mapping not found"));

        if (mapping.getAccountCode() != null) {
            return journalDetailRepository.findByAccountAndDateRange(
                    mapping.getAccountCode(),
                    LocalDate.of(1900, 1, 1),
                    header.getBaseDate());
        }

        return new ArrayList<>();
    }

    private BigDecimal calculateLineAmount(ReportLineMapping mapping, LocalDate baseDate) {
        if ("SUM".equals(mapping.getAggregationType())) {
            if (mapping.getAccountCode() != null) {
                // ?πÏ†ï Í≥ÑÏ†ï???îÏï° ?©ÏÇ∞ (ÏµúÏ†Å?îÎêú ÏøºÎ¶¨ ?¨Ïö©)
                return journalDetailRepository.findByAccountAndDateRange(
                        mapping.getAccountCode(),
                        LocalDate.of(1900, 1, 1),
                        baseDate)
                        .stream()
                        .map(this::calculateSignedAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        // FORMULA ??Ï∂îÍ? Î°úÏßÅ ?ÑÏöî (?¨Í∏∞???®Ïàú??
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateSignedAmount(JournalDetail detail) {
        // ?§Ï†ú ?¥ÏòÅ ?òÍ≤Ω?êÏÑú??Í≥ÑÏ†ï ?±Í≤©(Ï∞®Î????ÄÎ≥Ä?????∞Îùº Î∂Ä?∏Î? Í≤∞Ï†ï?¥Ïïº ??
        // ?¨Í∏∞?úÎäî ?®Ïàú Ï∞®Î?-?ÄÎ≥Ä?ºÎ°ú Ï≤òÎ¶¨ (?êÏÇ∞ Í≥ÑÏ†ï Í∏∞Ï?)
        return "DEBIT".equals(detail.getDrcrType()) ? detail.getAmount() : detail.getAmount().negate();
    }
}
