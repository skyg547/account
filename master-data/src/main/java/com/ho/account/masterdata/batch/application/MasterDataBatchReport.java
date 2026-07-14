package com.ho.account.masterdata.batch.application;

import com.ho.account.masterdata.core.application.pipeline.MasterDataValidityReport;
import java.time.LocalDate;

public record MasterDataBatchReport(
        LocalDate asOfDate,
        long activeAccountSubjects,
        long activeDepartments,
        long activeProducts,
        long activeBusinessPartners) {

    public static MasterDataBatchReport from(MasterDataValidityReport report) {
        return new MasterDataBatchReport(
                report.asOfDate(),
                report.activeAccountSubjects(),
                report.activeDepartments(),
                report.activeProducts(),
                report.activeBusinessPartners());
    }
}