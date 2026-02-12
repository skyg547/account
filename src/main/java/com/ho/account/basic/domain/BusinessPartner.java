package com.ho.account.basic.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate; // Import LocalDate
import java.util.List; // Import List

@Entity
@Table(name = "business_partners")
public class BusinessPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String businessPartnerCode;

    @Column(nullable = false, length = 100)
    private String businessPartnerName;

    @Column(length = 20)
    private String registrationNumber; // 사업자등록번호

    @Column(length = 50)
    private String ceoName;

    @Column(length = 50)
    private String businessType; // 업태

    @Column(length = 50)
    private String businessItem; // 종목

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartnerType partnerType;

    @Column(nullable = false)
    private Boolean useYn = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskRating riskRating = RiskRating.LOW;

    @Column(nullable = false)
    private LocalDate validFrom = LocalDate.now();

    @Column(nullable = false)
    private LocalDate validTo = LocalDate.MAX;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "businessPartner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessPartnerAccount> accounts;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBusinessPartnerCode() { return businessPartnerCode; }
    public void setBusinessPartnerCode(String businessPartnerCode) { this.businessPartnerCode = businessPartnerCode; }

    public String getBusinessPartnerName() { return businessPartnerName; }
    public void setBusinessPartnerName(String businessPartnerName) { this.businessPartnerName = businessPartnerName; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getCeoName() { return ceoName; }
    public void setCeoName(String ceoName) { this.ceoName = ceoName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getBusinessItem() { return businessItem; }
    public void setBusinessItem(String businessItem) { this.businessItem = businessItem; }

    public PartnerType getPartnerType() { return partnerType; }
    public void setPartnerType(PartnerType partnerType) { this.partnerType = partnerType; }

    public Boolean getUseYn() { return useYn; }
    public void setUseYn(Boolean useYn) { this.useYn = useYn; }

    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }

    public RiskRating getRiskRating() { return riskRating; }
    public void setRiskRating(RiskRating riskRating) { this.riskRating = riskRating; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<BusinessPartnerAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<BusinessPartnerAccount> accounts) {
        this.accounts = accounts;
    }

    public enum PartnerType {
        CUSTOMER, VENDOR, BANK, OTHER_BP
    }

    public enum KycStatus {
        PENDING, APPROVED, REJECTED, REVIEW_REQUIRED
    }

    public enum RiskRating {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
