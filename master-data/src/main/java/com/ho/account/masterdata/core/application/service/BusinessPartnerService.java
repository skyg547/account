package com.ho.account.masterdata.core.application.service;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.usecase.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.port.out.BusinessPartnerPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BusinessPartnerService implements BusinessPartnerUseCase {

    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public BusinessPartnerService(BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    /**
     * 거래처를 신규 등록합니다.
     *
     * <p>이 메서드는 DTO나 JPA Repository를 모릅니다. command를 도메인 엔티티로 바꾸고,
     * 중복 코드와 유효기간 기본값 같은 업무 규칙만 적용한 뒤 출력 포트로 저장을 위임합니다.</p>
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

    // 전체 거래처 조회
    @Transactional(readOnly = true)
    public List<BusinessPartner> getAllBusinessPartners() {
        return businessPartnerPersistencePort.findAll();
    }

    // 사용 중인 거래처만 조회
    @Transactional(readOnly = true)
    public List<BusinessPartner> getActiveBusinessPartners() {
        return businessPartnerPersistencePort.findByUseYnTrue();
    }

    // 거래처 상세 조회 (코드)
    @Transactional(readOnly = true)
    public Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode) {
        return businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode);
    }

    // 거래처 검색 (이름)
    @Transactional(readOnly = true)
    public List<BusinessPartner> searchBusinessPartnersByName(String name) {
        return businessPartnerPersistencePort.findByBusinessPartnerNameContaining(name);
    }

    /**
     * 거래처를 수정합니다.
     *
     * <p>현재는 같은 row를 수정하는 운영형 변경입니다. 과거 장부 재현까지 필요한 핵심 필드 변경은
     * 이후 별도 SCD2 버전 생성 유스케이스로 분리할 수 있습니다.</p>
     */
    public BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command) {
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

        businessPartner.setBusinessPartnerName(command.businessPartnerName());
        businessPartner.setRegistrationNumber(command.registrationNumber());
        businessPartner.setCeoName(command.ceoName());
        businessPartner.setBusinessType(command.businessType());
        businessPartner.setBusinessItem(command.businessItem());
        if (command.partnerType() != null) {
            businessPartner.setPartnerType(command.partnerType());
        }
        if (command.useYn() != null) {
            businessPartner.setUseYn(command.useYn());
        }
        if (command.kycStatus() != null) {
            businessPartner.setKycStatus(command.kycStatus());
        }
        if (command.riskRating() != null) {
            businessPartner.setRiskRating(command.riskRating());
        }
        businessPartner.setValidFrom(command.validFrom());
        businessPartner.setValidTo(command.validTo());
        MasterDataValidityPolicy.applyDefaultWindow(businessPartner::getValidFrom, businessPartner::setValidFrom,
                businessPartner::getValidTo, businessPartner::setValidTo);

        return businessPartnerPersistencePort.save(businessPartner);
    }

    // 거래처 삭제 (논리적 삭제)
    public void deleteBusinessPartner(Long id) {
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));
        businessPartner.setUseYn(false);
        businessPartnerPersistencePort.save(businessPartner);
    }
}
