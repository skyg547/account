package com.ho.account.asset.domain;

import com.ho.account.basic.domain.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 자산 이력 (Asset History)
 * 자산의 부서 이동, 상태 변경, 수선 등의 이력을 관리합니다.
 */
@Entity
@Table(name = "asset_histories")
@Getter
@Setter
@NoArgsConstructor
public class AssetHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(nullable = false)
    private String historyType; // DEPT_CHANGE, STATUS_CHANGE, REPAIR, ACQUISITION, DISPOSAL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_dept_code")
    private Department oldDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_dept_code")
    private Department newDepartment;

    @Column(length = 20)
    private String oldStatus;

    @Column(length = 20)
    private String newStatus;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime eventAt;

    private String auditUser;

    @PrePersist
    protected void onCreate() {
        if (eventAt == null) eventAt = LocalDateTime.now();
    }
}
