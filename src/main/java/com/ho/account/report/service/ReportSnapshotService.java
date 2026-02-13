package com.ho.account.report.service;

import com.ho.account.journal.domain.JournalDetail;
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
     * 특정 기준일의 보고서를 생성하고 스냅샷으로 저장합니다.
     */
    public ReportSnapshotHeader createSnapshot(String reportType, LocalDate baseDate, String description) {
        List<ReportLineMapping> mappings = reportMappingService.getActiveMappings(reportType, baseDate);

        ReportSnapshotHeader header = new ReportSnapshotHeader();
        header.setReportType(reportType);
        header.setBaseDate(baseDate);
        header.setStatus(ReportSnapshotHeader.SnapshotStatus.DRAFT);
        header.setDescription(description);

        // 버전 결정
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
     * 보고서 스냅샷의 특정 라인 금액을 구성하는 전표 상세 내역을 조회합니다 (Drill-through).
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
                // 특정 계정의 잔액 합산 (최적화된 쿼리 사용)
                return journalDetailRepository.findByAccountAndDateRange(
                        mapping.getAccountCode(),
                        LocalDate.of(1900, 1, 1),
                        baseDate)
                        .stream()
                        .map(this::calculateSignedAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        // FORMULA 등 추가 로직 필요 (여기선 단순화)
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateSignedAmount(JournalDetail detail) {
        // 실제 운영 환경에서는 계정 성격(차변형/대변형)에 따라 부호를 결정해야 함
        // 여기서는 단순 차변-대변으로 처리 (자산 계정 기준)
        return "DEBIT".equals(detail.getDrcrType()) ? detail.getAmount() : detail.getAmount().negate();
    }
}
