package com.ho.account.masterdata.core.domain.changerequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 留덉뒪??蹂寃쎌슂泥??꾨찓?몄엯?덈떎.
 *
 * <p>留덉뒪???곗씠?곕뒗 ?꾪몴, ?먯옣, 蹂닿퀬媛 怨듯넻?쇰줈 誘욧퀬 ?곕뒗 湲곗??낅땲?? 洹몃옒???댁쁺?먭? 諛붾줈
 * 媛믪쓣 諛붽씀吏 ?딄퀬 "?붿껌 -> ?뱀씤/諛섎젮 -> ?곸슜" ?곹깭瑜??④꺼??媛먯궗 異붿쟻??媛?ν빀?덈떎.</p>
 */
@Entity
@Table(name = "master_data_change_requests")
public class MasterDataChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MasterDataType targetType;

    @Column(nullable = false, length = 100)
    private String targetKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeStatus status;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private Integer requestedVersion;

    @Column(nullable = false, length = 80)
    private String requestedBy;

    @Column(length = 80)
    private String approvedBy;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String reason;

    @Column(columnDefinition = "CLOB")
    private String payloadJson;

    protected MasterDataChangeRequest() {
    }

    public MasterDataChangeRequest(MasterDataType targetType, String targetKey, ChangeType changeType,
            LocalDate effectiveDate, Integer requestedVersion, String requestedBy, String reason, String payloadJson) {
        this.targetType = require(targetType, "???留덉뒪???좏삎? ?꾩닔?낅땲??");
        this.targetKey = requireText(targetKey, "????ㅻ뒗 ?꾩닔?낅땲??");
        this.changeType = require(changeType, "蹂寃??좏삎? ?꾩닔?낅땲??");
        this.effectiveDate = require(effectiveDate, "?곸슜?쇱? ?꾩닔?낅땲??");
        this.requestedVersion = requirePositiveVersion(requestedVersion);
        this.requestedBy = requireText(requestedBy, "?붿껌?먮뒗 ?꾩닔?낅땲??");
        this.reason = reason;
        this.payloadJson = payloadJson;
        this.status = ChangeStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(String approver) {
        ensureRequested("?뱀씤? REQUESTED ?곹깭?먯꽌留?媛?ν빀?덈떎.");
        String normalizedApprover = requireText(approver, "?뱀씤?먮뒗 ?꾩닔?낅땲??");
        if (requestedBy.equals(normalizedApprover)) {
            throw new IllegalStateException("?붿껌?먯? ?뱀씤?먮뒗 媛숈쓣 ???놁뒿?덈떎.");
        }
        this.approvedBy = normalizedApprover;
        this.approvedAt = LocalDateTime.now();
        this.status = ChangeStatus.APPROVED;
    }

    public void reject(String approver, String rejectReason) {
        ensureRequested("諛섎젮??REQUESTED ?곹깭?먯꽌留?媛?ν빀?덈떎.");
        this.approvedBy = requireText(approver, "諛섎젮?먮뒗 ?꾩닔?낅땲??");
        this.approvedAt = LocalDateTime.now();
        this.reason = rejectReason;
        this.status = ChangeStatus.REJECTED;
    }

    public void markApplied() {
        if (status != ChangeStatus.APPROVED) {
            throw new IllegalStateException("?곸슜 ?꾨즺 泥섎━??APPROVED ?곹깭?먯꽌留?媛?ν빀?덈떎.");
        }
        this.status = ChangeStatus.APPLIED;
    }

    public boolean isReadyToApply(LocalDate today) {
        return status == ChangeStatus.APPROVED && !today.isBefore(effectiveDate);
    }

    private void ensureRequested(String message) {
        if (status != ChangeStatus.REQUESTED) {
            throw new IllegalStateException(message);
        }
    }

    private static <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Integer requirePositiveVersion(Integer version) {
        if (version == null || version < 1) {
            throw new IllegalArgumentException("?붿껌 踰꾩쟾? 1 ?댁긽?댁뼱???⑸땲??");
        }
        return version;
    }

    public Long getId() { return id; }
    public MasterDataType getTargetType() { return targetType; }
    public String getTargetKey() { return targetKey; }
    public ChangeType getChangeType() { return changeType; }
    public ChangeStatus getStatus() { return status; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public Integer getRequestedVersion() { return requestedVersion; }
    public String getRequestedBy() { return requestedBy; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public String getReason() { return reason; }
    public String getPayloadJson() { return payloadJson; }

    public enum MasterDataType {
        ACCOUNT_SUBJECT, BUSINESS_PARTNER, DEPARTMENT, PRODUCT, CURRENCY, EXCHANGE_RATE, FISCAL_PERIOD
    }

    public enum ChangeType {
        CREATE, UPDATE, DEACTIVATE
    }

    public enum ChangeStatus {
        REQUESTED, APPROVED, REJECTED, APPLIED
    }
}
