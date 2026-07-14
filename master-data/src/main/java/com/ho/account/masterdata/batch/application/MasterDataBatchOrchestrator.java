package com.ho.account.masterdata.batch.application;

import com.ho.account.masterdata.core.application.pipeline.MasterDataValidityReportPipeline;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Batch 인바운드 오케스트레이터입니다.
 *
 * <p>Job/스케줄러가 전달한 기준일을 core pipeline에 넘기고 결과를 Batch 출력 모델로
 * 변환할 뿐, 유효성 판단이나 집계 연산은 직접 수행하지 않습니다.</p>
 */
@Component
public class MasterDataBatchOrchestrator {

    private final MasterDataValidityReportPipeline masterDataValidityReportPipeline;

    public MasterDataBatchOrchestrator(MasterDataValidityReportPipeline masterDataValidityReportPipeline) {
        this.masterDataValidityReportPipeline = masterDataValidityReportPipeline;
    }

    public MasterDataBatchReport createDailyValidityReport(LocalDate asOfDate) {
        return MasterDataBatchReport.from(masterDataValidityReportPipeline.createDailyValidityReport(asOfDate));
    }
}