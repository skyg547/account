package com.ho.account.basic.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // LocalDateTime 추가

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일시

    @Column(length = 50)
    private String auditUser; // 감사 사용자

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code")
    private Department parent;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DepartmentType type;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    public enum DepartmentType {
        COST_CENTER, // 비용 센터
        PROFIT_CENTER, // 이익 센터
        SUPPORT, // 지원 부서
        OTHER // 기타
    }

    // Getters and Setters
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

    public Department getParent() {
        return parent;
    }

    public void setParent(Department parent) {
        this.parent = parent;
    }

    public DepartmentType getType() {
        return type;
    }

    public void setType(DepartmentType type) {
        this.type = type;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

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
}
