package com.ho.account.masterdata.batch.application;

import com.ho.account.masterdata.core.application.pipeline.MasterDataValidityReportPipeline;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class MasterDataBatchOrchestrator {

    private final MasterDataValidityReportPipeline masterDataValidityReportPipeline;

    public MasterDataBatchOrchestrator(MasterDataValidityReportPipeline masterDataValidityReportPipeline) {
        this.masterDataValidityReportPipeline = masterDataValidityReportPipeline;
    }

    public MasterDataBatchReport createDailyValidityReport(LocalDate asOfDate) {
        return masterDataValidityReportPipeline.createDailyValidityReport(asOfDate);
    }
}

