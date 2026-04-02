package com.ho.account.reconciliation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 차이 사유 코드(Difference Reason Code) 엔티티
 * 대사 과정에서 발생한 차이에 대한 사유를 분류하고 관리합니다.
 */
@Entity
@Table(name = "difference_reason_codes")
public class DifferenceReasonCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code; // 사유 코드 (예: BANK_FEE, TIMING_DIFF, DATA_ENTRY_ERR)

    @Column(nullable = false, length = 100)
    private String name; // 사유명 (예: "은행 수수료", "시차 발생", "데이터 입력 오류")

    @Column(length = 500)
    private String description; // 상세 설명

    @Column(nullable = false)
    private boolean isAdjustable; // 조정 전표 생성이 필요한 차이인지 여부

    @Column(nullable = false)
    private boolean isActive = true; // 활성화 여부

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String auditUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getter 및 Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAdjustable() {
        return isAdjustable;
    }

    public void setAdjustable(boolean adjustable) {
        isAdjustable = adjustable;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    public void setAuditUser(String auditUser) {
        this.auditUser = auditUser;
    }
}
