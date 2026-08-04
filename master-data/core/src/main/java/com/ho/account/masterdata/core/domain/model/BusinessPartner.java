package com.ho.account.masterdata.core.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 거래처의 업무 규칙을 표현하는 순수 도메인 모델입니다.
 *
 * <p>이 클래스에는 JPA 같은 저장 기술의 어노테이션이 없습니다. 도메인 모델이 테이블 구조를
 * 알게 되면 저장 방식을 바꿀 때 업무 규칙까지 흔들리기 때문입니다. 저장 어댑터는 이 객체를
 * 별도의 영속성 엔티티로 변환하고, 이 객체는 코드 불변성·유효기간·종료 같은 업무 의미에만
 * 집중합니다.</p>
 */
public class BusinessPartner {

    public static final LocalDate OPEN_ENDED_VALID_TO = LocalDate.of(9999, 12, 31);
    private static final String DEFAULT_AUDIT_USER = "SYSTEM";

    private Long id;
    private String businessPartnerCode;
    private String businessPartnerName;
    private String registrationNumber;
    private String ceoName;
    private String businessType;
    private String businessItem;
    private PartnerType partnerType = PartnerType.OTHER_BP;
    private Boolean useYn = true;
    private KycStatus kycStatus = KycStatus.PENDING;
    private RiskRating riskRating = RiskRating.LOW;
    private LocalDate validFrom = LocalDate.now();
    private LocalDate validTo = OPEN_ENDED_VALID_TO;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser = DEFAULT_AUDIT_USER;
    private List<BusinessPartnerAccount> accounts = List.of();

    /**
     * 기존 호출자의 단계별 객체 조립을 깨뜨리지 않기 위한 호환 생성자입니다.
     *
     * <p>신규 업무 코드에서는 필수 값과 유효기간을 한 번에 검증하는 {@link #create}를 사용해야
     * 합니다. 빈 객체는 잠시 불완전할 수 있어 aggregate를 저장하기 전에 factory를 거치는 것이
     * 안전합니다.</p>
     */
    public BusinessPartner() {
    }

    private BusinessPartner(
            Long id,
            String businessPartnerCode,
            String businessPartnerName,
            String registrationNumber,
            String ceoName,
            String businessType,
            String businessItem,
            PartnerType partnerType,
            Boolean useYn,
            KycStatus kycStatus,
            RiskRating riskRating,
            LocalDate validFrom,
            LocalDate validTo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String auditUser,
            List<BusinessPartnerAccount> accounts) {
        this.id = id;
        this.businessPartnerCode = requireText(businessPartnerCode, "거래처 코드는 필수입니다.");
        this.businessPartnerName = requireText(businessPartnerName, "거래처명은 필수입니다.");
        this.registrationNumber = trimToNull(registrationNumber);
        this.ceoName = trimToNull(ceoName);
        this.businessType = trimToNull(businessType);
        this.businessItem = trimToNull(businessItem);
        this.partnerType = Objects.requireNonNull(partnerType, "거래처 유형은 필수입니다.");
        this.useYn = Objects.requireNonNull(useYn, "사용 여부는 필수입니다.");
        this.kycStatus = Objects.requireNonNull(kycStatus, "KYC 상태는 필수입니다.");
        this.riskRating = Objects.requireNonNull(riskRating, "위험 등급은 필수입니다.");
        requireValidityWindow(validFrom, validTo);
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.auditUser = defaultIfBlank(auditUser, DEFAULT_AUDIT_USER);
        replaceAccounts(accounts);
    }

    /**
     * 새 거래처 aggregate를 생성합니다.
     *
     * <p>필수 값과 SCD2 유효기간을 도메인 진입점에서 함께 검증하면 Controller나 저장 어댑터마다
     * 서로 다른 검증을 중복 구현하지 않아도 됩니다. 선택 값이 없는 경우의 기본값도 여기서
     * 결정하므로 어느 인바운드 어댑터가 호출해도 같은 거래처가 만들어집니다.</p>
     */
    public static BusinessPartner create(
            String businessPartnerCode,
            String businessPartnerName,
            String registrationNumber,
            String ceoName,
            String businessType,
            String businessItem,
            PartnerType partnerType,
            Boolean useYn,
            KycStatus kycStatus,
            RiskRating riskRating,
            LocalDate validFrom,
            LocalDate validTo) {
        LocalDateTime now = LocalDateTime.now();
        return new BusinessPartner(
                null,
                businessPartnerCode,
                businessPartnerName,
                registrationNumber,
                ceoName,
                businessType,
                businessItem,
                partnerType != null ? partnerType : PartnerType.OTHER_BP,
                useYn != null ? useYn : Boolean.TRUE,
                kycStatus != null ? kycStatus : KycStatus.PENDING,
                riskRating != null ? riskRating : RiskRating.LOW,
                validFrom != null ? validFrom : LocalDate.now(),
                validTo != null ? validTo : OPEN_ENDED_VALID_TO,
                now,
                now,
                DEFAULT_AUDIT_USER,
                List.of());
    }

    /**
     * 저장되어 있던 거래처 aggregate를 업무 객체로 복원합니다.
     *
     * <p>이 메서드는 저장 기술을 도메인에 노출하지 않습니다. 대신 저장 어댑터가 읽은 모든 값을
     * 전달해 과거의 식별자와 감사 시각을 잃지 않으면서도 도메인 불변식을 다시 확인하게 합니다.</p>
     */
    public static BusinessPartner reconstitute(
            Long id,
            String businessPartnerCode,
            String businessPartnerName,
            String registrationNumber,
            String ceoName,
            String businessType,
            String businessItem,
            PartnerType partnerType,
            Boolean useYn,
            KycStatus kycStatus,
            RiskRating riskRating,
            LocalDate validFrom,
            LocalDate validTo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String auditUser,
            List<BusinessPartnerAccount> accounts) {
        return new BusinessPartner(
                id,
                businessPartnerCode,
                businessPartnerName,
                registrationNumber,
                ceoName,
                businessType,
                businessItem,
                partnerType,
                useYn,
                kycStatus,
                riskRating,
                validFrom,
                validTo,
                createdAt,
                updatedAt,
                auditUser,
                accounts);
    }

    /**
     * 현재 버전의 업무 식별자를 유지하면서 다음 SCD2 버전을 준비합니다.
     *
     * <p>거래처 코드는 버전이 바뀌어도 같은 거래처를 잇는 업무 식별자입니다. 따라서 이 규칙은
     * 서비스의 조건문이 아니라 aggregate 자체가 지켜야 합니다. 새 버전을 먼저 완전히 검증한 뒤
     * 서비스가 기존 버전을 종료하므로, 잘못된 요청 때문에 현재 버전만 먼저 닫히는 일도 막습니다.</p>
     */
    public BusinessPartner createNextVersion(
            String requestedBusinessPartnerCode,
            String businessPartnerName,
            String registrationNumber,
            String ceoName,
            String businessType,
            String businessItem,
            PartnerType partnerType,
            Boolean useYn,
            KycStatus kycStatus,
            RiskRating riskRating,
            LocalDate newValidFrom,
            LocalDate newValidTo) {
        if (requestedBusinessPartnerCode != null
                && !businessPartnerCode.equals(requestedBusinessPartnerCode.trim())) {
            throw new IllegalArgumentException("거래처 코드는 SCD2 버전 수정 시 변경할 수 없습니다.");
        }
        requireValidityWindow(newValidFrom, newValidTo);
        LocalDate previousValidTo = newValidFrom.minusDays(1);
        if (previousValidTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("새로운 유효 시작일이 기존 시작일보다 빠를 수 없습니다.");
        }
        if (!isActiveAt(LocalDate.now())) {
            throw new IllegalStateException("활성 거래처 버전만 수정할 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<BusinessPartnerAccount> nextVersionAccounts = accounts.stream()
                .map(BusinessPartnerAccount::copyForNextVersion)
                .toList();
        return new BusinessPartner(
                null,
                businessPartnerCode,
                firstNonNull(businessPartnerName, this.businessPartnerName),
                firstNonNull(registrationNumber, this.registrationNumber),
                firstNonNull(ceoName, this.ceoName),
                firstNonNull(businessType, this.businessType),
                firstNonNull(businessItem, this.businessItem),
                firstNonNull(partnerType, this.partnerType),
                firstNonNull(useYn, Boolean.TRUE),
                firstNonNull(kycStatus, this.kycStatus),
                firstNonNull(riskRating, this.riskRating),
                newValidFrom,
                newValidTo,
                now,
                now,
                auditUser,
                nextVersionAccounts);
    }

    /**
     * 기준일이 이 버전의 닫힌 구간([validFrom, validTo]) 안에 있는지 확인합니다.
     */
    public boolean isValid(LocalDate date) {
        return date != null
                && !date.isBefore(validFrom)
                && !date.isAfter(validTo);
    }

    /**
     * 기준일에 사용할 수 있는 현재 버전인지 확인합니다.
     *
     * <p>유효기간과 업무 사용 여부는 서로 다른 조건이므로 둘을 함께 확인해야 미래 버전이나
     * 명시적으로 비활성화된 거래처를 현재 거래에 잘못 사용하지 않습니다.</p>
     */
    public boolean isActiveAt(LocalDate date) {
        return Boolean.TRUE.equals(useYn) && isValid(date);
    }

    /**
     * 새 SCD2 버전으로 이어지도록 현재 버전의 유효기간만 닫습니다.
     *
     * <p>버전 교체는 거래처 자체의 사용 중단과 다릅니다. 여기서 {@code useYn}까지 false로
     * 바꾸면 미래 시행일로 수정한 순간부터 기존 거래처가 조회되지 않는 공백이 생깁니다.
     * 따라서 update 흐름은 이 메서드를 사용하고, 실제 비활성화 흐름만 {@link #terminate}를
     * 사용합니다.</p>
     */
    public void closeVersion(LocalDate endDate) {
        requireEndDateWithinCurrentWindow(endDate);
        if (endDate.equals(validTo)) {
            return;
        }
        validTo = endDate;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 이 SCD2 버전을 지정일에 종료합니다.
     *
     * <p>종료일은 기존 유효기간 밖으로 이동할 수 없습니다. 종료 시 validTo가 endDate로 조정되어
     * [validFrom, endDate] 기간 동안의 과거/당일 조회가 당시 active 상태로 정상 재현되며,
     * endDate 이후 기준일 조회에서는 유효기간 초과로 자동 비활성 처리됩니다.</p>
     */
    public void terminate(LocalDate endDate) {
        requireEndDateWithinCurrentWindow(endDate);
        if (endDate.equals(validTo) && !validTo.equals(OPEN_ENDED_VALID_TO)) {
            return;
        }
        validTo = endDate;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 거래처를 명시적으로 비활성화(정지) 처리합니다.
     */
    public void deactivate() {
        if (!Boolean.TRUE.equals(useYn)) {
            return;
        }
        useYn = false;
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    /**
     * 이전 테스트·호출자의 단계별 조립 호환용입니다. 신규 코드는 factory를 사용하세요.
     */
    @Deprecated
    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessPartnerCode() {
        return businessPartnerCode;
    }

    @Deprecated
    public void setBusinessPartnerCode(String businessPartnerCode) {
        this.businessPartnerCode = requireText(businessPartnerCode, "거래처 코드는 필수입니다.");
    }

    public String getBusinessPartnerName() {
        return businessPartnerName;
    }

    @Deprecated
    public void setBusinessPartnerName(String businessPartnerName) {
        this.businessPartnerName = requireText(businessPartnerName, "거래처명은 필수입니다.");
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    @Deprecated
    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = trimToNull(registrationNumber);
    }

    public String getCeoName() {
        return ceoName;
    }

    @Deprecated
    public void setCeoName(String ceoName) {
        this.ceoName = trimToNull(ceoName);
    }

    public String getBusinessType() {
        return businessType;
    }

    @Deprecated
    public void setBusinessType(String businessType) {
        this.businessType = trimToNull(businessType);
    }

    public String getBusinessItem() {
        return businessItem;
    }

    @Deprecated
    public void setBusinessItem(String businessItem) {
        this.businessItem = trimToNull(businessItem);
    }

    public PartnerType getPartnerType() {
        return partnerType;
    }

    @Deprecated
    public void setPartnerType(PartnerType partnerType) {
        this.partnerType = Objects.requireNonNull(partnerType, "거래처 유형은 필수입니다.");
    }

    public Boolean getUseYn() {
        return useYn;
    }

    @Deprecated
    public void setUseYn(Boolean useYn) {
        this.useYn = Objects.requireNonNull(useYn, "사용 여부는 필수입니다.");
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    @Deprecated
    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = Objects.requireNonNull(kycStatus, "KYC 상태는 필수입니다.");
    }

    public RiskRating getRiskRating() {
        return riskRating;
    }

    @Deprecated
    public void setRiskRating(RiskRating riskRating) {
        this.riskRating = Objects.requireNonNull(riskRating, "위험 등급은 필수입니다.");
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    @Deprecated
    public void setValidFrom(LocalDate validFrom) {
        if (validFrom == null) {
            throw new IllegalArgumentException("유효 시작일은 필수입니다.");
        }
        requireValidityWindow(validFrom, validTo);
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    @Deprecated
    public void setValidTo(LocalDate validTo) {
        if (validTo == null) {
            throw new IllegalArgumentException("유효 종료일은 필수입니다.");
        }
        requireValidityWindow(validFrom, validTo);
        this.validTo = validTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getAuditUser() {
        return auditUser;
    }

    @Deprecated
    public void setAuditUser(String auditUser) {
        this.auditUser = defaultIfBlank(auditUser, DEFAULT_AUDIT_USER);
    }

    public List<BusinessPartnerAccount> getAccounts() {
        return accounts;
    }

    /**
     * 기존 조립 코드 호환용입니다. 전달 목록을 복사해 외부에서 aggregate 내부 목록을 바꾸지 못하게 합니다.
     */
    @Deprecated
    public void setAccounts(List<BusinessPartnerAccount> accounts) {
        replaceAccounts(accounts);
    }

    private void replaceAccounts(List<BusinessPartnerAccount> accounts) {
        List<BusinessPartnerAccount> safeAccounts = accounts == null ? List.of() : List.copyOf(accounts);
        long mainAccountCount = safeAccounts.stream()
                .filter(BusinessPartnerAccount::isMainAccount)
                .count();
        // 지급 계좌를 자동 선택하는 소비자는 주계좌가 둘이면 결정할 수 없으므로 aggregate가 모호성을 차단합니다.
        if (mainAccountCount > 1) {
            throw new IllegalArgumentException("거래처에는 주계좌를 하나만 지정할 수 있습니다.");
        }
        safeAccounts.forEach(account -> account.attachTo(this));
        this.accounts = safeAccounts;
    }

    boolean hasAnotherMainAccount(BusinessPartnerAccount candidate) {
        return accounts.stream()
                .anyMatch(account -> account != candidate && account.isMainAccount());
    }

    private static void requireValidityWindow(LocalDate validFrom, LocalDate validTo) {
        // 누락된 업무 날짜는 프로그래밍 오류(NPE)가 아니라 호출자가 고칠 수 있는 검증 오류로 알립니다.
        if (validFrom == null) {
            throw new IllegalArgumentException("유효 시작일은 필수입니다.");
        }
        if (validTo == null) {
            throw new IllegalArgumentException("유효 종료일은 필수입니다.");
        }
        if (validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("유효 종료일은 유효 시작일보다 빠를 수 없습니다.");
        }
    }

    private void requireEndDateWithinCurrentWindow(LocalDate endDate) {
        // 날짜 누락은 호출자의 입력 오류이므로 NPE가 아니라 일관된 validation 예외로 알립니다.
        if (endDate == null) {
            throw new IllegalArgumentException("거래처 종료일은 필수입니다.");
        }
        if (endDate.isBefore(validFrom)) {
            throw new IllegalArgumentException("거래처 종료일은 유효 시작일보다 빠를 수 없습니다.");
        }
        if (endDate.isAfter(validTo)) {
            throw new IllegalArgumentException("거래처 종료일은 현재 유효 종료일보다 늦을 수 없습니다.");
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

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static <T> T firstNonNull(T requested, T fallback) {
        return requested != null ? requested : fallback;
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
