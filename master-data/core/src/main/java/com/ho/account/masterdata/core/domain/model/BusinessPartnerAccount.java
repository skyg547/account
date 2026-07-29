package com.ho.account.masterdata.core.domain.model;

import java.time.LocalDateTime;

/**
 * 거래처 aggregate에 속한 정산 계좌 도메인 모델입니다.
 *
 * <p>계좌는 거래처의 일부이지만 테이블 연관관계나 지연 로딩 같은 저장 기술은 알지 않습니다.
 * 이렇게 분리하면 계좌번호 필수 여부와 주계좌 의미를 데이터베이스 없이도 테스트할 수 있고,
 * 저장 방식이 바뀌어도 업무 규칙은 그대로 유지됩니다.</p>
 */
public class BusinessPartnerAccount {

    private Long id;
    private BusinessPartner businessPartner;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String swiftCode;
    private boolean mainAccount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 기존 단계별 조립 코드를 위한 호환 생성자입니다. 신규 코드는 {@link #create}를 사용하세요.
     */
    public BusinessPartnerAccount() {
    }

    private BusinessPartnerAccount(
            Long id,
            String bankName,
            String accountNumber,
            String accountHolder,
            String swiftCode,
            boolean mainAccount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.bankName = requireText(bankName, "은행명은 필수입니다.");
        this.accountNumber = requireText(accountNumber, "계좌번호는 필수입니다.");
        this.accountHolder = requireText(accountHolder, "예금주는 필수입니다.");
        this.swiftCode = trimToNull(swiftCode);
        this.mainAccount = mainAccount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 계좌를 생성하며 필수 계좌 정보를 한 번에 검증합니다.
     *
     * <p>필수값 검증을 aggregate 내부에 두면 API뿐 아니라 배치나 메시지 입력도 빈 계좌번호를
     * 우회 저장할 수 없습니다.</p>
     */
    public static BusinessPartnerAccount create(
            String bankName,
            String accountNumber,
            String accountHolder,
            String swiftCode,
            boolean mainAccount) {
        LocalDateTime now = LocalDateTime.now();
        return new BusinessPartnerAccount(
                null,
                bankName,
                accountNumber,
                accountHolder,
                swiftCode,
                mainAccount,
                now,
                now);
    }

    /**
     * 저장되어 있던 계좌를 식별자와 감사 시각까지 보존해 복원합니다.
     *
     * <p>복원 factory도 필수 정보를 검증하므로 손상된 저장 데이터가 정상 aggregate인 것처럼
     * 애플리케이션 계층으로 흘러가는 것을 빠르게 발견할 수 있습니다.</p>
     */
    public static BusinessPartnerAccount reconstitute(
            Long id,
            String bankName,
            String accountNumber,
            String accountHolder,
            String swiftCode,
            boolean mainAccount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new BusinessPartnerAccount(
                id,
                bankName,
                accountNumber,
                accountHolder,
                swiftCode,
                mainAccount,
                createdAt,
                updatedAt);
    }

    /**
     * 다음 SCD2 거래처 버전에 속할 새 계좌 행을 만듭니다.
     *
     * <p>계좌 값을 그대로 승계하더라도 기술 식별자까지 재사용하면 기존 과거 계좌 행의 FK가 새
     * 거래처 버전으로 이동할 수 있습니다. 그러면 과거 거래처를 조회했을 때 당시 지급 계좌가
     * 사라집니다. 따라서 업무 값은 복사하되 ID는 비우고 생성·수정 시각을 새로 부여해 두 버전이
     * 서로 다른 자식 행을 소유하도록 합니다.</p>
     */
    BusinessPartnerAccount copyForNextVersion() {
        return create(
                bankName,
                accountNumber,
                accountHolder,
                swiftCode,
                mainAccount);
    }

    /**
     * 계좌가 어느 aggregate에 속하는지는 거래처 루트만 결정합니다.
     *
     * <p>외부 코드가 임의로 소유자를 바꾸지 못하도록 package-private으로 두고,
     * {@link BusinessPartner}가 계좌 목록을 받아들일 때만 연결합니다.</p>
     */
    void attachTo(BusinessPartner businessPartner) {
        if (businessPartner == null) {
            throw new IllegalArgumentException("계좌가 속한 거래처는 필수입니다.");
        }
        if (this.businessPartner != null && this.businessPartner != businessPartner) {
            throw new IllegalStateException("거래처 계좌는 다른 거래처로 이동할 수 없습니다.");
        }
        this.businessPartner = businessPartner;
    }

    public Long getId() {
        return id;
    }

    @Deprecated
    public void setId(Long id) {
        this.id = id;
    }

    public BusinessPartner getBusinessPartner() {
        return businessPartner;
    }

    /**
     * 이전 호출자 호환용이며 실제 aggregate 연결은 거래처 루트가 수행해야 합니다.
     */
    @Deprecated
    public void setBusinessPartner(BusinessPartner businessPartner) {
        attachTo(businessPartner);
    }

    public String getBankName() {
        return bankName;
    }

    @Deprecated
    public void setBankName(String bankName) {
        this.bankName = requireText(bankName, "은행명은 필수입니다.");
        touch();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    @Deprecated
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = requireText(accountNumber, "계좌번호는 필수입니다.");
        touch();
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    @Deprecated
    public void setAccountHolder(String accountHolder) {
        this.accountHolder = requireText(accountHolder, "예금주는 필수입니다.");
        touch();
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    @Deprecated
    public void setSwiftCode(String swiftCode) {
        this.swiftCode = trimToNull(swiftCode);
        touch();
    }

    public boolean isMainAccount() {
        return mainAccount;
    }

    @Deprecated
    public void setMainAccount(boolean mainAccount) {
        // 이미 aggregate에 연결된 뒤에도 주계좌 단일성 규칙을 우회할 수 없게 루트에 확인합니다.
        if (mainAccount
                && businessPartner != null
                && businessPartner.hasAnotherMainAccount(this)) {
            throw new IllegalStateException("거래처에는 주계좌를 하나만 지정할 수 있습니다.");
        }
        this.mainAccount = mainAccount;
        touch();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Deprecated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Deprecated
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private void touch() {
        if (createdAt != null) {
            updatedAt = LocalDateTime.now();
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
