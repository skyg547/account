package com.ho.account.asset.domain;

import com.ho.account.masterdata.core.domain.model.Department;
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
    @JoinColumn(name = "asset_id")
    private FixedAsset fixedAsset;

    private String historyType; // TRANSFER, DISPOSAL, DEPRECIATION, STATUS_CHANGE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_dept_code", referencedColumnName = "dept_code")
    private Department oldDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_dept_code", referencedColumnName = "dept_code")
    private Department newDepartment;

    private String oldStatus;
    private String newStatus;
    private String description;

    private LocalDateTime eventAt;
    private String auditUser;
}
