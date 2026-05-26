package com.risk.credit.core.domain.result;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [Entity] 신용 리스크 배치 감사 추적 (Batch Audit) 엔티티
 * 배치 작업의 실행 이력, 처리 건수, 성공/실패 여부 및 집계된 리스크 지표를 기록합니다.
 *
 * [초보자를 위한 개념 설명]
 * 배치 감사(Audit)는 시스템이 밤새 어떤 일을 했는지 기록하는 '일기장'과 같습니다. 
 * "어제 몇 명의 고객에 대해 리스크를 계산했는지", "그중 실패한 건은 없는지", 
 * "우리 은행 전체의 익스포저(EAD)가 얼마로 집계되었는지" 등을 한눈에 확인하여 
 * 산출 데이터의 무결성을 검증하는 데 사용합니다.
 */
@Entity
@Table(name = "cr_batch_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrBatchAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 배치 산출 기준 일자 */
    @Column(name = "base_date", nullable = false)
    private java.time.LocalDate baseDate;

    /** 배치 작업 명칭 (예: creditRiskCalculationJob) */
    @Column(name = "job_name", nullable = false)
    private String jobName;

    /** 총 처리 대상 건수 */
    @Column(name = "total_count")
    private Integer totalCount;

    /** 산출 성공 건수 */
    @Column(name = "success_count")
    private Integer successCount;

    /** 산출 실패 건수 */
    @Column(name = "fail_count")
    private Integer failCount;

    /** 해당 배치에서 산출된 총 익스포저 (EAD) 합계 */
    @Column(name = "total_ead", precision = 19, scale = 4)
    private BigDecimal totalEad;

    /** 해당 배치에서 산출된 총 위험가중자산 (RWA) 합계 */
    @Column(name = "total_rwa", precision = 19, scale = 4)
    private BigDecimal totalRwa;

    /** 배치 현재 상태 (STARTED, COMPLETED, FAILED 등) */
    @Column(name = "status", nullable = false)
    private String status;

    /** 배치 시작 일시 */
    @Column(name = "start_at")
    private LocalDateTime startAt;

    /** 배치 종료 일시 */
    @Column(name = "end_at")
    private LocalDateTime endAt;
}
