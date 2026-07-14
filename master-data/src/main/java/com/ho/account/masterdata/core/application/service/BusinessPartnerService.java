package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 거래처(Business Partner) 마스터 데이터를 관리하는 서비스입니다.
 * 헥사고날 아키텍처의 Application Service 계층이며, SCD2 이력 관리 정책을 따릅니다.
 */
@Service
@Transactional
public class BusinessPartnerService implements BusinessPartnerUseCase {

    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public BusinessPartnerService(BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    /**
     * 신규 거래처를 등록합니다.
     */
    public BusinessPartner createBusinessPartner(BusinessPartnerCommand command) {
        BusinessPartner businessPartner = command.toEntity();
        if (businessPartnerPersistencePort.existsByBusinessPartnerCode(businessPartner.getBusinessPartnerCode())) {
            throw new IllegalArgumentException("이미 존재하는 거래처 코드입니다: " + businessPartner.getBusinessPartnerCode());
        }
        MasterDataValidityPolicy.applyDefaultWindow(businessPartner::getValidFrom, businessPartner::setValidFrom,
                businessPartner::getValidTo, businessPartner::setValidTo);
        return businessPartnerPersistencePort.save(businessPartner);
    }

    /**
     * 모든 거래처를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<BusinessPartner> getAllBusinessPartners() {
        return businessPartnerPersistencePort.findAll();
    }

    /**
     * 활성화된 거래처만 조회합니다. (Legacy UseYn 플래그 기반)
     */
    @Transactional(readOnly = true)
    public List<BusinessPartner> getActiveBusinessPartners() {
        return businessPartnerPersistencePort.findByUseYnTrue();
    }

    /**
     * 거래처 코드로 단건 조회합니다.
     */
    @Transactional(readOnly = true)
    public Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode) {
        return businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode);
    }

    /**
     * 거래처 이름으로 검색합니다.
     */
    @Transactional(readOnly = true)
    public List<BusinessPartner> searchBusinessPartnersByName(String name) {
        return businessPartnerPersistencePort.findByBusinessPartnerNameContaining(name);
    }

    /**
     * 거래처 정보를 수정합니다. (SCD2 원칙에 따라 기존 버전을 종료하고 새 버전을 생성합니다)
     */
    public BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command) {
        BusinessPartner currentActive = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

        // SCD2: 기존 활성 버전 종료
        LocalDate newValidFrom = command.validFrom() != null ? command.validFrom() : LocalDate.now();
        LocalDate oldValidTo = newValidFrom.minusDays(1);
        
        if (oldValidTo.isBefore(currentActive.getValidFrom())) {
            throw new IllegalArgumentException("새로운 유효 시작일이 기존 시작일보다 빠를 수 없습니다.");
        }
        if (command.businessPartnerCode() != null
                && !command.businessPartnerCode().equals(currentActive.getBusinessPartnerCode())) {
            throw new IllegalArgumentException("거래처 코드는 SCD2 버전 수정 시 변경할 수 없습니다.");
        }
        if (!Boolean.TRUE.equals(currentActive.getUseYn()) || !currentActive.isValid(LocalDate.now())) {
            throw new IllegalStateException("활성 거래처 버전만 수정할 수 있습니다.");
        }
        
        // 현재 활성 이력을 종료
        currentActive.terminate(oldValidTo);
        businessPartnerPersistencePort.save(currentActive);

        // SCD2: 새로운 버전 생성
        BusinessPartner newVersion = buildNextVersion(currentActive, command);
        newVersion.setBusinessPartnerCode(currentActive.getBusinessPartnerCode());
        newVersion.setValidFrom(newValidFrom);
        newVersion.setValidTo(command.validTo() != null ? command.validTo() : LocalDate.of(9999, 12, 31));
        
        return businessPartnerPersistencePort.save(newVersion);
    }

    /**
     * 거래처를 논리적으로 삭제(비활성화)합니다.
     */
    public void deleteBusinessPartner(Long id) {
        deleteBusinessPartner(id, LocalDate.now());
    }

    @Override
    public void deleteBusinessPartner(Long id, LocalDate effectiveDate) {
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

        LocalDate terminationDate = MasterDataValidityPolicy.requireTerminationDate(
                effectiveDate, businessPartner.getValidFrom(), businessPartner.getValidTo());
        businessPartner.terminate(terminationDate);
        businessPartnerPersistencePort.save(businessPartner);
    }

    private BusinessPartner buildNextVersion(BusinessPartner currentActive, BusinessPartnerCommand command) {
        BusinessPartner newVersion = new BusinessPartner();
        newVersion.setBusinessPartnerName(firstNonNull(
                command.businessPartnerName(),
                currentActive.getBusinessPartnerName()));
        newVersion.setRegistrationNumber(firstNonNull(
                command.registrationNumber(),
                currentActive.getRegistrationNumber()));
        newVersion.setCeoName(firstNonNull(command.ceoName(), currentActive.getCeoName()));
        newVersion.setBusinessType(firstNonNull(command.businessType(), currentActive.getBusinessType()));
        newVersion.setBusinessItem(firstNonNull(command.businessItem(), currentActive.getBusinessItem()));
        newVersion.setPartnerType(firstNonNull(command.partnerType(), currentActive.getPartnerType()));
        newVersion.setUseYn(firstNonNull(command.useYn(), Boolean.TRUE));
        newVersion.setKycStatus(firstNonNull(command.kycStatus(), currentActive.getKycStatus()));
        newVersion.setRiskRating(firstNonNull(command.riskRating(), currentActive.getRiskRating()));
        newVersion.setAuditUser(currentActive.getAuditUser());
        return newVersion;
    }

    private <T> T firstNonNull(T requested, T fallback) {
        return requested != null ? requested : fallback;
    }
}
