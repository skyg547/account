package com.ho.account.basic.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // LocalDateTime 추가

/**
 * 부서(Department/Cost Center) 엔티티
 * 조직 구조를 관리하며, 비용 센터(Cost Center) 또는 이익 센터(Profit Center) 역할을 수행함.
 */
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

    /**
     * 부서의 이름.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 상위 부서. 계층 구조를 나타냅니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code", referencedColumnName = "code")
    private Department parent;

    /**
     * 부서의 유형 (예: 비용 센터, 이익 센터, 지원 부서).
     */
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

    // Getter 및 Setter
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

    @Deprecated
    public void setUseYn(boolean useYn) {
        // Compatibility shim for legacy tests. Current model does not track useYn.
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.auditUser == null) {
            this.auditUser = "SYSTEM";
        }
        if (this.type == null) {
            this.type = DepartmentType.OTHER;
        }
        if (this.validFrom == null) {
            this.validFrom = LocalDate.now();
        }
        if (this.validTo == null) {
            this.validTo = LocalDate.of(9999, 12, 31);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
