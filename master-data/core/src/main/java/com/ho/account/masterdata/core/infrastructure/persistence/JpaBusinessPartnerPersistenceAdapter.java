package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerAccount;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.BusinessPartnerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래처 도메인 aggregate와 JPA 저장 모델 사이를 번역하는 출력 어댑터입니다.
 *
 * <p>이 클래스 밖의 application/domain 코드는 {@link BusinessPartnerJpaEntity}를 알지 못합니다.
 * 반대로 Spring Data repository도 순수 도메인 객체를 직접 저장하지 않습니다. 이 명시적 번역
 * 지점 덕분에 DB 컬럼 변경과 업무 규칙 변경을 서로 독립적으로 검토할 수 있습니다.</p>
 */
@Component
public class JpaBusinessPartnerPersistenceAdapter implements BusinessPartnerPersistencePort {

    private final BusinessPartnerRepository businessPartnerRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaBusinessPartnerPersistenceAdapter(BusinessPartnerRepository businessPartnerRepository) {
        this.businessPartnerRepository = businessPartnerRepository;
    }

    @Override
    public boolean existsByBusinessPartnerCode(String businessPartnerCode) {
        return businessPartnerRepository.existsByBusinessPartnerCode(businessPartnerCode);
    }

    @Override
    public Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode) {
        return businessPartnerRepository.findByBusinessPartnerCode(businessPartnerCode)
                .map(this::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<BusinessPartner> findByBusinessPartnerCodeForUpdate(String businessPartnerCode) {
        return businessPartnerRepository.findByBusinessPartnerCode(businessPartnerCode)
                .map(this::toFreshDomain);
    }

    @Override
    public List<BusinessPartner> findAllByBusinessPartnerCodeIn(Collection<String> businessPartnerCodes) {
        if (businessPartnerCodes == null || businessPartnerCodes.isEmpty()) {
            return List.of();
        }
        return businessPartnerRepository.findActiveByBusinessPartnerCodeIn(businessPartnerCodes, LocalDate.now())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<BusinessPartner> findEffectiveByBusinessPartnerCode(
            String businessPartnerCode,
            LocalDate effectiveDate) {
        return businessPartnerRepository
                .findEffectiveByBusinessPartnerCode(businessPartnerCode, effectiveDate)
                .map(this::toDomain);
    }

    @Override
    public Optional<BusinessPartner> findById(Long id) {
        return businessPartnerRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<BusinessPartner> findByIdForUpdate(Long id) {
        return businessPartnerRepository.findById(id).map(this::toFreshDomain);
    }

    @Override
    public Optional<String> findBusinessKeyById(Long id) {
        return businessPartnerRepository.findBusinessKeyById(id);
    }

    @Override
    public List<BusinessPartner> findAll() {
        return businessPartnerRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<BusinessPartner> findByUseYnTrue() {
        return businessPartnerRepository.findByUseYnTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<BusinessPartner> searchActiveByName(String name, LocalDate asOfDate) {
        return businessPartnerRepository.searchActiveByName(name, asOfDate).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public BusinessPartner save(BusinessPartner businessPartner) {
        BusinessPartnerJpaEntity entity = toJpaEntity(businessPartner);
        // 기존 구간 종료를 먼저 flush해야 새 IDENTITY 행의 즉시 INSERT가 중복 기간으로 거부되지 않습니다.
        BusinessPartnerJpaEntity persisted = entity.getId() == null
                ? businessPartnerRepository.save(entity)
                : businessPartnerRepository.saveAndFlush(entity);
        return toDomain(persisted);
    }

    private BusinessPartner toFreshDomain(BusinessPartnerJpaEntity entity) {
        // 상위 트랜잭션이 미리 읽은 객체도 키 잠금 대기 후 DB의 최신 구간으로 다시 검증합니다.
        entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return toDomain(entity);
    }

    /**
     * 저장 기술의 객체를 도메인 aggregate로 복원합니다.
     *
     * <p>{@code reconstitute}는 신규 거래처를 만드는 factory와 달리 이미 검증되어 저장된 식별자와
     * 감사 시각을 그대로 되살리는 용도입니다. 따라서 조회가 도메인 생성 이벤트나 현재 시각을
     * 새로 만들지 않습니다.</p>
     */
    private BusinessPartner toDomain(BusinessPartnerJpaEntity entity) {
        List<BusinessPartnerAccount> accounts = entity.getAccounts().stream()
                .map(this::toDomainAccount)
                .toList();

        return BusinessPartner.reconstitute(
                entity.getId(),
                entity.getBusinessPartnerCode(),
                entity.getBusinessPartnerName(),
                entity.getRegistrationNumber(),
                entity.getCeoName(),
                entity.getBusinessType(),
                entity.getBusinessItem(),
                entity.getPartnerType(),
                entity.getUseYn(),
                entity.getKycStatus(),
                entity.getRiskRating(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getAuditUser(),
                accounts);
    }

    private BusinessPartnerAccount toDomainAccount(BusinessPartnerAccountJpaEntity entity) {
        return BusinessPartnerAccount.reconstitute(
                entity.getId(),
                entity.getBankName(),
                entity.getAccountNumber(),
                entity.getAccountHolder(),
                entity.getSwiftCode(),
                entity.isMainAccount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /**
     * 도메인 상태를 같은 테이블 구조의 JPA aggregate로 복사합니다.
     *
     * <p>식별자와 생성 시각도 함께 복사하므로 기존 행 저장은 INSERT가 아니라 merge/update로
     * 처리됩니다. 자식 계좌의 부모 FK 연결은 {@code replaceAccounts}가 persistence 내부에서
     * 완성합니다.</p>
     */
    private BusinessPartnerJpaEntity toJpaEntity(BusinessPartner domain) {
        BusinessPartnerJpaEntity entity = new BusinessPartnerJpaEntity();
        entity.setId(domain.getId());
        entity.setBusinessPartnerCode(domain.getBusinessPartnerCode());
        entity.setBusinessPartnerName(domain.getBusinessPartnerName());
        entity.setRegistrationNumber(domain.getRegistrationNumber());
        entity.setCeoName(domain.getCeoName());
        entity.setBusinessType(domain.getBusinessType());
        entity.setBusinessItem(domain.getBusinessItem());
        entity.setPartnerType(domain.getPartnerType());
        entity.setUseYn(domain.getUseYn());
        entity.setKycStatus(domain.getKycStatus());
        entity.setRiskRating(domain.getRiskRating());
        entity.setValidFrom(domain.getValidFrom());
        entity.setValidTo(domain.getValidTo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());

        List<BusinessPartnerAccountJpaEntity> accountEntities =
                safeAccounts(domain.getAccounts()).stream()
                        .map(this::toJpaAccountEntity)
                        .toList();
        entity.replaceAccounts(accountEntities);
        return entity;
    }

    private BusinessPartnerAccountJpaEntity toJpaAccountEntity(BusinessPartnerAccount domain) {
        BusinessPartnerAccountJpaEntity entity = new BusinessPartnerAccountJpaEntity();
        entity.setId(domain.getId());
        entity.setBankName(domain.getBankName());
        entity.setAccountNumber(domain.getAccountNumber());
        entity.setAccountHolder(domain.getAccountHolder());
        entity.setSwiftCode(domain.getSwiftCode());
        entity.setMainAccount(domain.isMainAccount());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private List<BusinessPartnerAccount> safeAccounts(List<BusinessPartnerAccount> accounts) {
        // 과거 호출자가 null 컬렉션을 넘겨도 "계좌 없음"으로 정규화해 매핑 분기를 한 곳에 둡니다.
        return accounts == null ? List.of() : accounts;
    }
}
