package com.ho.account.mart.core.domain.external.kap;

import lombok.*;

/**
 * [순수 도메인 모델] 외부 신용등급 마스터 (Rating Grade Master)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RatingGradeMaster {
    private Long id;
    private String ratingAgency;
    private String gradeCode;
    private String gradeName;
    private Integer gradeScore;
    private Boolean isActive;
}
