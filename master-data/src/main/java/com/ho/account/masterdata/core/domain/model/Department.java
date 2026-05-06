package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 부서(Department/Cost Center) 엔티티
 * 조직 구조를 관리하며 SCD2(Slowly Changing Dimension Type 2) 방식을 사용하여 조직 개편 이력을 추적합니다.
 */
@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_dept_code_valid", columnList = "code, valid_from, valid_to")
})
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    /**
     * 부서의 명칭
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 상위 부서 연관관계 (ID 기반)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    /**
     * 부서의 유형 (예: 비용 센터, 이익 센터, 지원 부서 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DepartmentType type;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성일시

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일시

    @Column(length = 50)
    private String auditUser; // 감사 사용자

    public enum DepartmentType {
        COST_CENTER, // 비용 센터
        PROFIT_CENTER, // 이익 센터
        SUPPORT, // 지원 부서
        OTHER // 기타
    }

    /**
     * 특정 시점에 유효한지 확인
     */
    public boolean isValid(LocalDate date) {
        return (date.isEqual(validFrom) || date.isAfter(validFrom)) && (date.isEqual(validTo) || date.isBefore(validTo));
    }

    /**
     * 현재 이력을 종료
     */
    public void terminate(LocalDate endDate) {
        this.validTo = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    @Deprecated
    public void setUseYn(boolean useYn) {
        // Compatibility shim for legacy tests.
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
