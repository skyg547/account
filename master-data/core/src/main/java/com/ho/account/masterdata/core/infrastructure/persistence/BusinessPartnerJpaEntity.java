package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.BusinessPartner.KycStatus;
import com.ho.account.masterdata.core.domain.model.BusinessPartner.PartnerType;
import com.ho.account.masterdata.core.domain.model.BusinessPartner.RiskRating;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code business_partners} 테이블의 모양과 JPA 생명주기만 표현하는 영속성 엔티티입니다.
 *
 * <p>초보자 관점에서 도메인 {@code BusinessPartner}는 "거래처가 어떤 규칙으로 행동하는가"를
 * 설명하고, 이 클래스는 "그 상태를 어느 컬럼에 저장하는가"만 설명합니다. 두 책임을 분리하면
 * 도메인 객체가 Spring Data/JPA 프록시나 지연 로딩 규칙에 의존하지 않습니다.</p>
 */
@Entity
@Table(name = "business_partners", indexes = {
        @Index(name = "idx_bp_code_valid", columnList = "business_partner_code, valid_from, valid_to")
})
public class BusinessPartnerJpaEntity {

    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_partner_code", nullable = false, length = 20)
    private String businessPartnerCode;

    @Column(name = "business_partner_name", nullable = false, length = 100)
    private String businessPartnerName;

    @Column(name = "registration_number", length = 20)
    private String registrationNumber;

    @Column(name = "ceo_name", length = 50)
    private String ceoName;

    @Column(name = "business_type", length = 50)
    private String businessType;

    @Column(name = "business_item", length = 50)
    private String businessItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false, length = 20)
    private PartnerType partnerType;

    @Column(name = "use_yn", nullable = false)
    private Boolean useYn = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 30)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating", nullable = false, length = 20)
    private RiskRating riskRating = RiskRating.LOW;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom = LocalDate.now();

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo = OPEN_ENDED_DATE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "audit_user", length = 50)
    private String auditUser;

    @OneToMany(mappedBy = "businessPartner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessPartnerAccountJpaEntity> accounts = new ArrayList<>();

    protected BusinessPartnerJpaEntity() {
        // JPA가 조회 결과를 복원할 때 사용하는 기본 생성자입니다.
    }

    /**
     * 신규 행에만 저장 기술 기본값을 채웁니다.
     *
     * <p>업무 기본값은 도메인이 먼저 결정하지만, 오래된 호출 경로에서 null이 들어와도 기존
     * 스키마의 NOT NULL 계약을 깨지 않도록 종전 JPA 콜백의 방어 동작을 그대로 보존합니다.</p>
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (auditUser == null) {
            auditUser = "SYSTEM";
        }
        if (partnerType == null) {
            partnerType = PartnerType.OTHER_BP;
        }
        if (useYn == null) {
            useYn = true;
        }
        if (kycStatus == null) {
            kycStatus = KycStatus.PENDING;
        }
        if (riskRating == null) {
            riskRating = RiskRating.LOW;
        }
        if (validFrom == null) {
            validFrom = LocalDate.now();
        }
        if (validTo == null) {
            validTo = OPEN_ENDED_DATE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // 수정 시각은 DB 저장 사건의 메타데이터이므로 영속성 엔티티가 책임집니다.
        updatedAt = LocalDateTime.now();
    }

    /**
     * 자식 계좌를 교체하면서 양방향 JPA 연관관계의 FK 소유자를 함께 맞춥니다.
     *
     * <p>도메인 계좌는 aggregate root의 객체 참조를 몰라도 되지만, 관계형 DB는 각 계좌 행에
     * {@code business_partner_id}가 필요합니다. 이 차이를 영속성 경계 안에서만 해결합니다.</p>
     */
    public void replaceAccounts(List<BusinessPartnerAccountJpaEntity> replacement) {
        accounts.clear();
        if (replacement == null) {
            return;
        }
        replacement.forEach(account -> {
            account.attachTo(this);
            accounts.add(account);
        });
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessPartnerCode() {
        return businessPartnerCode;
    }

    public void setBusinessPartnerCode(String businessPartnerCode) {
        this.businessPartnerCode = businessPartnerCode;
    }

    public String getBusinessPartnerName() {
        return businessPartnerName;
    }

    public void setBusinessPartnerName(String businessPartnerName) {
        this.businessPartnerName = businessPartnerName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getCeoName() {
        return ceoName;
    }

    public void setCeoName(String ceoName) {
        this.ceoName = ceoName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessItem() {
        return businessItem;
    }

    public void setBusinessItem(String businessItem) {
        this.businessItem = businessItem;
    }

    public PartnerType getPartnerType() {
        return partnerType;
    }

    public void setPartnerType(PartnerType partnerType) {
        this.partnerType = partnerType;
    }

    public Boolean getUseYn() {
        return useYn;
    }

    public void setUseYn(Boolean useYn) {
        this.useYn = useYn;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public RiskRating getRiskRating() {
        return riskRating;
    }

    public void setRiskRating(RiskRating riskRating) {
        this.riskRating = riskRating;
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

    public List<BusinessPartnerAccountJpaEntity> getAccounts() {
        return accounts;
    }
}
