package com.risk.credit.core.domain.exposure;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [Exposure] 고객 신용등급 변동 이력 (Customer Rating History)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 전이행렬(Transition Matrix)을 만들기 위해서는 과거에 고객의 등급이 어떻게 변해왔는지
 * 기록이 필요합니다. 예를 들어 "작년에 1등급이었던 고객 중 올해 2등급으로 떨어진 고객은 몇 명인가?"를
 * 계산할 때 이 데이터를 사용합니다.
 */
@Entity
@Table(name = "cr_cust_rating_hist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrCustomerRatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_id", nullable = false)
    private CrCustomer customer;

    /** 변경된 신용 등급 */
    @Column(name = "rating_cd", length = 10, nullable = false)
    private String ratingCode;

    /** 등급 산출/적용 기준일 */
    @Column(name = "base_dt", nullable = false)
    private LocalDate baseDate;

    /** 데이터 생성 일시 */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
