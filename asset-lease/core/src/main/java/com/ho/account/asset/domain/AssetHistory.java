package com.ho.account.asset.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 자산 이력 엔티티
 */
@Entity
@Table(name = "asset_histories")
@Getter @Setter
@NoArgsConstructor
public class AssetHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset fixedAsset;

    @Column(nullable = false, length = 50)
    private String historyType; // TRANSFER, DISPOSAL, DEPRECIATION, STATUS_CHANGE

    @Column(name = "old_dept_code", length = 20)
    private String oldDepartmentCode;

    @Column(name = "new_dept_code", length = 20)
    private String newDepartmentCode;

    @Column(length = 20)
    private String oldStatus;
    @Column(length = 20)
    private String newStatus;
    private String description;

    @Column(nullable = false)
    private LocalDateTime eventAt;
    @Column(nullable = false, length = 50)
    private String auditUser;
}
