package com.ho.account.report.service;

import com.ho.account.report.domain.ReportLineMapping;
import com.ho.account.report.repository.ReportLineMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ReportMappingService {

    private final ReportLineMappingRepository reportLineMappingRepository;

    @Autowired
    public ReportMappingService(ReportLineMappingRepository reportLineMappingRepository) {
        this.reportLineMappingRepository = reportLineMappingRepository;
    }

    /**
     * 특정 기준일의 활성화된 보고서 라인 매핑 정보를 조회합니다.
     */
    public List<ReportLineMapping> getActiveMappings(String reportType, LocalDate asOfDate) {
        return reportLineMappingRepository.findActiveByReportTypeAsOfDate(reportType, asOfDate);
    }

    /**
     * 새로운 보고서 라인 매핑을 생성하거나 기존 매핑을 업데이트합니다 (SCD2).
     */
    public ReportLineMapping createOrUpdateMapping(ReportLineMapping newMapping) {
        // 기존 매핑이 있다면 해당 버전의 유효 종료일을 설정하고 새 버전을 생성합니다.
        List<ReportLineMapping> existing = reportLineMappingRepository.findByLineCode(newMapping.getLineCode());

        for (ReportLineMapping mapping : existing) {
            if (mapping.getValidToDate() == null || mapping.getValidToDate().isAfter(newMapping.getValidFromDate())) {
                mapping.setValidToDate(newMapping.getValidFromDate().minusDays(1));
                reportLineMappingRepository.save(mapping);
            }
        }

        // 새 버전의 버전 번호 설정
        int nextVersion = existing.stream()
                .mapToInt(ReportLineMapping::getVersion)
                .max()
                .orElse(0) + 1;

        newMapping.setVersion(nextVersion);
        return reportLineMappingRepository.save(newMapping);
    }
}
