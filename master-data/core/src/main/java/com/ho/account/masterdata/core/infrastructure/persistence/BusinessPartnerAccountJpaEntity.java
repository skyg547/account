package com.ho.account.masterdata.core.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 거래처 계좌 한 건을 {@code business_partner_accounts} 테이블에 저장하는 JPA 전용 모델입니다.
 *
 * <p>계좌번호 같은 값의 업무 의미와 검증은 도메인 객체가 담당하고, 이 클래스는 FK·컬럼 길이·
 * 저장 시각처럼 관계형 저장소에만 필요한 정보를 담당합니다.</p>
 */
@Entity
@Table(name = "business_partner_accounts")
public class BusinessPartnerAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_partner_id", nullable = false)
    private BusinessPartnerJpaEntity businessPartner;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 100)
    private String accountHolder;

    @Column(name = "swift_code", length = 20)
    private String swiftCode;

    @Column(name = "is_main_account", nullable = false)
    private boolean mainAccount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected BusinessPartnerAccountJpaEntity() {
        // JPA가 조회 결과를 만들기 위한 기본 생성자입니다.
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // 도메인 규칙이 아니라 저장 행의 갱신 메타데이터이므로 JPA 콜백에서 기록합니다.
        updatedAt = LocalDateTime.now();
    }

    void attachTo(BusinessPartnerJpaEntity businessPartner) {
        // FK의 실제 소유자는 자식 엔티티이므로 cascade 저장 전에 부모 참조를 반드시 연결합니다.
        this.businessPartner = businessPartner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public boolean isMainAccount() {
        return mainAccount;
    }

    public void setMainAccount(boolean mainAccount) {
        this.mainAccount = mainAccount;
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
}
