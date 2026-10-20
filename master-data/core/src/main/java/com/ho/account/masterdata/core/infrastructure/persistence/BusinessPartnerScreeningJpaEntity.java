package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.model.BusinessPartner.KycStatus;
import com.ho.account.masterdata.core.domain.model.BusinessPartner.RiskRating;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Current screening snapshot, keyed by the stable business code rather than an SCD2 row ID. */
@Entity
@Table(name = "business_partner_screenings")
@Getter
@Setter
@NoArgsConstructor
public class BusinessPartnerScreeningJpaEntity {

    @Id
    @Column(name = "business_partner_code", nullable = false, length = 20)
    private String businessPartnerCode;

    @Column(name = "jurisdiction", nullable = false, length = 2)
    private String jurisdiction;

    @Column(name = "identifier_type", nullable = false, length = 64)
    private String identifierType;

    @Column(name = "identifier", nullable = false, length = 128)
    private String identifier;

    @Column(name = "evidence_reference", nullable = false, length = 512)
    private String evidenceReference;

    @Column(name = "evidence_expires_on")
    private LocalDate evidenceExpiresOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private KycStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating", nullable = false, length = 20)
    private RiskRating riskRating;

    @Column(name = "submitted_by", nullable = false, length = 128)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_by", length = 128)
    private String reviewedBy;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
