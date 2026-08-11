package com.ho.account.mart.core.domain.external.kap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * [Domain Entity] 한국자산평가(KAP) 외부 신용평가 도메인 엔티티.
 *
 * <p><strong>교육적 주석 (Pedagogical Comments):</strong></p>
 * <ul>
 *   <li><strong>헥사고날 아키텍처 (Port and Adapter Pattern) 준수:</strong>
 *       도메인 엔티티는 프레임워크나 특정 영속성 기술(JPA/Hibernate)에 의존해서는 안 됩니다.
 *       기존에는 {@code @Entity}, {@code @Table}, {@code @Column} 등 JPA 기술 어노테이션이 도메인 모델에 침범하여
 *       포트 앤 어댑터 아키텍처 원칙을 위반했었습니다.</li>
 *   <li><strong>도메인 모델의 순수성 (Pure Java POJO):</strong>
 *       JPA 어노테이션을 전면 제거하여 도메인 모델을 기술 독립적인 Pure POJO로 격리함으로써
 *       비즈니스 로직 단위 테스트 용이성, 영속성 기술 교체(RDB -> NoSQL/In-Memory 등)의 유연성,
 *       그리고 도메인 캡슐화 능력을 극대화합니다.</li>
 *   <li><strong>영속성 분리:</strong> DB 스키마 매핑 정보는 Infrastructure 레이어의
 *       {@code KapExternalRatingEntity}로 전담 이동하였으며, Data Mapper 패턴으로 변환을 처리합니다.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KapExternalRating {

    private Long id;

    private String customerId;

    private String evalAgency;

    private String ratingGrade;

    private LocalDate baseDate;
}

