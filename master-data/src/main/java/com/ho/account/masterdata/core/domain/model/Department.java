package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // LocalDateTime 異붽?

/**
 * 遺??Department/Cost Center) ?뷀떚??
 * 議곗쭅 援ъ“瑜?愿由ы븯硫? 鍮꾩슜 ?쇳꽣(Cost Center) ?먮뒗 ?댁씡 ?쇳꽣(Profit Center) ??븷???섑뻾??
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // ?앹꽦?쇱떆

    @Column(nullable = false)
    private LocalDateTime updatedAt; // ?섏젙?쇱떆

    @Column(length = 50)
    private String auditUser; // 媛먯궗 ?ъ슜??

    /**
     * 遺?쒖쓽 ?대쫫.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * ?곸쐞 遺?? 怨꾩링 援ъ“瑜??섑??낅땲??
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code", referencedColumnName = "code")
    private Department parent;

    /**
     * 遺?쒖쓽 ?좏삎 (?? 鍮꾩슜 ?쇳꽣, ?댁씡 ?쇳꽣, 吏??遺??.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DepartmentType type;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    public enum DepartmentType {
        COST_CENTER, // 鍮꾩슜 ?쇳꽣
        PROFIT_CENTER, // ?댁씡 ?쇳꽣
        SUPPORT, // 吏??遺??
        OTHER // 湲고?
    }

    // Getter 諛?Setter
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
