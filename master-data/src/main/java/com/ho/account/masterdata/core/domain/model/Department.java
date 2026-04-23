package com.ho.account.masterdata.core.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // LocalDateTime ?°ë¶½?

/**
 * ?ºÂ??Department/Cost Center) ????
 * °ê³—??´ÑŠâœç‘œ??¿Â?±Ñ‹ë¸¯? ??¾©????³ê½£(Cost Center) ??’— ??ì”¡ ??³ê½£(Profit Center) ??????‘ë»¾??
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // ??¹ê½¦??±ë–†

    @Column(nullable = false)
    private LocalDateTime updatedAt; // ??ì ™??±ë–†

    @Column(length = 50)
    private String auditUser; // ›ë??????

    /**
     * ?ºÂ??–ì“½ ???
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * ?¸ì ?ºÂ?? ?¾©??´ÑŠâœç‘œ??????…ë•²??
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code", referencedColumnName = "code")
    private Department parent;

    /**
     * ?ºÂ??–ì“½ ?ì‚ (?? ??¾©????³ê½£, ??ì”¡ ??³ê½£, Â???ºÂ??.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DepartmentType type;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    public enum DepartmentType {
        COST_CENTER, // ??¾©????³ê½£
        PROFIT_CENTER, // ??ì”¡ ??³ê½£
        SUPPORT, // Â???ºÂ??
        OTHER // ²ê³ ?
    }

    // Getter ?Setter
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
