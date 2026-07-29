package com.ho.account.masterdata.core.application.pipeline;

import java.time.LocalDate;

/**
 * 기준일별 기준정보 유효성 집계 결과입니다.
 *
 * <p>core가 이 모델을 소유하므로 API나 Batch 어댑터가 바뀌어도 업무 집계 결과의
 * 의미는 유지됩니다.</p>
 */
public record MasterDataValidityReport(
        LocalDate asOfDate,
        long activeAccountSubjects,
        long activeDepartments,
        long activeProducts,
        long activeBusinessPartners) {
}