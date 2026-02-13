package com.ho.account.report;

import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.service.JournalService;
import com.ho.account.report.domain.ReportLineMapping;
import com.ho.account.report.domain.ReportSnapshotDetail;
import com.ho.account.report.domain.ReportSnapshotHeader;
import com.ho.account.report.repository.ReportLineMappingRepository;
import com.ho.account.report.service.ReportSnapshotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FinancialReportingTest {

    @Autowired
    private ReportSnapshotService reportSnapshotService;

    @Autowired
    private ReportLineMappingRepository mappingRepository;

    @Autowired
    private JournalService journalService;

    @Test
    @DisplayName("보고서 스냅샷 생성 및 드릴다운 검증")
    void testReportSnapshotAndDrillDown() {
        // 1. 기초 데이터 (보고서 매핑) 설정
        ReportLineMapping mapping = new ReportLineMapping();
        mapping.setReportType("BS");
        mapping.setLineCode("CASH");
        mapping.setLineName("현금 및 현금성자산");
        mapping.setAccountCode("10100"); // 현금 계정
        mapping.setAggregationType("SUM");
        mapping.setDisplayOrder(1);
        mapping.setValidFromDate(LocalDate.of(2024, 1, 1));
        mappingRepository.save(mapping);

        // 2. 전표 데이터 생성 (추후 집계 대상)
        JournalEntry entry = new JournalEntry();
        entry.setSlipNo("JE-REPORT-001");
        entry.setAccountingDate(LocalDate.of(2024, 3, 1));
        entry.setSlipDate(LocalDate.of(2024, 3, 1));
        entry.setStatus(JournalEntryStatus.APPROVED);
        // ... 상세 내역 생략 (JournalService 사용 권장)

        // 3. 스냅샷 생성
        LocalDate baseDate = LocalDate.of(2024, 3, 31);
        ReportSnapshotHeader snapshot = reportSnapshotService.createSnapshot("BS", baseDate, "2024년 1분기 결산");

        assertNotNull(snapshot);
        assertFalse(snapshot.getDetails().isEmpty());

        // 4. 드릴다운 검증
        List<?> contributors = reportSnapshotService.getContributingJournals(snapshot.getSnapshotId(), "CASH");
        assertNotNull(contributors);
        // 집계된 전표가 있으면 검증 logic 추가
    }
}
