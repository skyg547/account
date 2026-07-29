package com.ho.account.masterdata.batch.application;

import com.ho.account.masterdata.core.application.pipeline.MasterDataValidityReport;
import java.time.LocalDate;

/**
 * Batch-owned projection written to the Spring Batch execution context.
 *
 * <p>The core report intentionally has no dependency on Spring Batch. This
 * adapter model lets operators inspect stable primitive values in job
 * metadata while the domain-facing pipeline remains reusable.</p>
 */
public record MasterDataBatchReport(
        LocalDate asOfDate,
        long activeAccountSubjects,
        long activeDepartments,
        long activeProducts,
        long activeBusinessPartners) {

    /**
     * Converts the core result at the inbound Batch boundary.
     */
    public static MasterDataBatchReport from(MasterDataValidityReport report) {
        return new MasterDataBatchReport(
                report.asOfDate(),
                report.activeAccountSubjects(),
                report.activeDepartments(),
                report.activeProducts(),
                report.activeBusinessPartners());
    }
}