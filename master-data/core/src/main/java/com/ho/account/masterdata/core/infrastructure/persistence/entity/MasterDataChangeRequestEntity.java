package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeType;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 기준정보 변경 요청(MasterDataChangeRequest) JPA 영속성 엔티티.
 * 
 * 🐣 [DDD & 영속성 모델 분리 교육적 주석]
 * master_data_change_requests 테이블 매핑 및 동시성 락(@Version)관리를 담당하는 엔티티입니다.
 */
@Entity
@Table(name = "master_data_change_requests")
@Getter
@Setter
@NoArgsConstructor
public class MasterDataChangeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MasterDataType targetType;

    @Column(nullable = false, length = 100)
    private String targetKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeStatus status;

    @Version
    @Column(nullable = false)
    private long lockVersion;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private Integer requestedVersion;

    @Column(nullable = false, length = 80)
    private String requestedBy;

    @Column(length = 80)
    private String approvedBy;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @Column(length = 120, unique = true)
    private String sourceReference;

    private LocalDateTime appliedAt;
}
