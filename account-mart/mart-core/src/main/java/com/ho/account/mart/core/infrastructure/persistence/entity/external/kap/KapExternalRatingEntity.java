package com.ho.account.mart.core.infrastructure.persistence.entity.external.kap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * [Infrastructure Layer Entity] 한국자산평가(KAP) 외부 신용평가 정보 JPA 엔티티.
 *
 * <p><strong>교육적 주석 (Pedagogical Comments):</strong></p>
 * <ul>
 *   <li><strong>헥사고날 아키텍처(Port and Adapter Pattern) 원칙:</strong> DB 기술(JPA, ORM, Table mapping)은
 *       외부 어댑터(Infrastructure Layer)의 관심사입니다.</li>
 *   <li><strong>도메인-영속성 분리:</strong> 도메인 모델(KapExternalRating)은 비즈니스 로직과 데이터 표현에 집중하는 Pure Java POJO이며,
 *       이 Entity 클래스가 RDB 테이블("kap_external_ratings")과의 O/R 매핑 책임을 전담합니다.</li>
 * </ul>
 */
@Entity
@Table(name = "kap_external_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KapExternalRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "eval_agency", nullable = false, length = 50)
    private String evalAgency;

    @Column(name = "rating_grade", nullable = false, length = 20)
    private String ratingGrade;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;
}
