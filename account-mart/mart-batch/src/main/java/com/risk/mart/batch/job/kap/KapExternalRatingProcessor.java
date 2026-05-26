package com.risk.mart.batch.job.kap;

import com.risk.mart.core.domain.external.kap.entity.KapExternalRating;
import com.risk.mart.core.domain.external.kap.service.KapRatingGradeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * KAP CSV 원본을 표준 엔티티로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class KapExternalRatingProcessor implements ItemProcessor<KapExternalRatingCsvRow, KapExternalRating> {

    private final KapRatingGradeResolver ratingGradeResolver;

    @Override
    public KapExternalRating process(@NonNull KapExternalRatingCsvRow item) {
        return KapExternalRating.builder()
                .customerId(item.getCustomerId())
                .evalAgency(item.getEvalAgency())
                .ratingGrade(ratingGradeResolver.resolveGradeCode(item.getRatingGrade()))
                .baseDate(LocalDate.parse(item.getBaseDate()))
                .build();
    }
}
