package com.risk.mart.batch.job.kap;

import lombok.Builder;
import lombok.Value;

/**
 * KAP 외부등급 CSV 원본 행.
 */
@Value
@Builder
public class KapExternalRatingCsvRow {
    String customerId;
    String evalAgency;
    String ratingGrade;
    String baseDate;
}
