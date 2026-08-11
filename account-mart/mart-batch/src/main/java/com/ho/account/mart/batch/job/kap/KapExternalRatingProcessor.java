package com.ho.account.mart.batch.job.kap;

import com.ho.account.mart.core.domain.external.kap.KapExternalRating;
import com.ho.account.mart.core.domain.external.kap.service.KapRatingGradeResolver;
import com.ho.account.mart.core.infrastructure.persistence.entity.external.kap.KapExternalRatingEntity;
import com.ho.account.mart.core.infrastructure.persistence.mapper.KapExternalRatingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * KAP CSV 원본을 도메인 룰 검증을 거쳐 JPA 영속성 엔티티로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class KapExternalRatingProcessor implements ItemProcessor<KapExternalRatingCsvRow, KapExternalRatingEntity> {

    private final KapRatingGradeResolver ratingGradeResolver;

    @Override
    public KapExternalRatingEntity process(@NonNull KapExternalRatingCsvRow item) {
        // 도메인 POJO 모델 생성을 통한 도메인 로직(등급 코드 변환) 수행
        KapExternalRating domain = KapExternalRating.builder()
                .customerId(item.getCustomerId())
                .evalAgency(item.getEvalAgency())
                .ratingGrade(ratingGradeResolver.resolveGradeCode(item.getRatingGrade()))
                .baseDate(LocalDate.parse(item.getBaseDate()))
                .build();

        // 영속성 레이어 저장을 위해 Mapper를 거쳐 Entity로 변환
        return KapExternalRatingMapper.toEntity(domain);
    }
}

