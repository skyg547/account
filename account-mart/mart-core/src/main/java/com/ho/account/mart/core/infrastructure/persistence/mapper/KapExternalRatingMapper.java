package com.ho.account.mart.core.infrastructure.persistence.mapper;

import com.ho.account.mart.core.domain.external.kap.KapExternalRating;
import com.ho.account.mart.core.infrastructure.persistence.entity.external.kap.KapExternalRatingEntity;

/**
 * [Mapper] KAP 외부 신용평가 도메인 POJO <-> JPA Entity 변환 매퍼.
 *
 * <p><strong>교육적 주석 (Pedagogical Comments):</strong></p>
 * 헥사고날 아키텍처에서는 Domain 모델과 Infrastructure JPA Entity 간의 결합도를 낮추기 위해
 * 데이터 매퍼(Data Mapper) 패턴을 적용합니다. 이 클래스는 두 객체 간 상호 변환 책임을 집니다.
 */
public class KapExternalRatingMapper {

    private KapExternalRatingMapper() {
        // 유틸리티/매퍼 클래스 인스턴스화 방지
    }

    public static KapExternalRating toDomain(KapExternalRatingEntity entity) {
        if (entity == null) {
            return null;
        }
        return KapExternalRating.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .evalAgency(entity.getEvalAgency())
                .ratingGrade(entity.getRatingGrade())
                .baseDate(entity.getBaseDate())
                .build();
    }

    public static KapExternalRatingEntity toEntity(KapExternalRating domain) {
        if (domain == null) {
            return null;
        }
        return KapExternalRatingEntity.builder()
                .id(domain.getId())
                .customerId(domain.getCustomerId())
                .evalAgency(domain.getEvalAgency())
                .ratingGrade(domain.getRatingGrade())
                .baseDate(domain.getBaseDate())
                .build();
    }
}
