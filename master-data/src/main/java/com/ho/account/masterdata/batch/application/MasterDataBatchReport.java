package com.ho.account.masterdata.batch.application;

import java.time.LocalDate;

public record MasterDataBatchReport(
        LocalDate asOfDate,
        long activeAccountSubjects,
        long activeDepartments,
        long activeProducts,
        long activeBusinessPartners) {
}
