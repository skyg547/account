package com.ho.account.masterdata.core.application.service;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.masterdata.core.application.usecase.BusinessPartnerUseCase;
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

    // 거래처 생성
    public BusinessPartner createBusinessPartner(BusinessPartner businessPartner) {
        if (businessPartnerPersistencePort.existsByBusinessPartnerCode(businessPartner.getBusinessPartnerCode())) {
            throw new IllegalArgumentException("이미 존재하는 거래처 코드입니다: " + businessPartner.getBusinessPartnerCode());
        }
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

    // 거래처 정보 수정
    public BusinessPartner updateBusinessPartner(Long id, BusinessPartner businessPartnerDetails) {
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

        businessPartner.setBusinessPartnerName(businessPartnerDetails.getBusinessPartnerName());
        businessPartner.setRegistrationNumber(businessPartnerDetails.getRegistrationNumber());
        businessPartner.setCeoName(businessPartnerDetails.getCeoName());
        businessPartner.setBusinessType(businessPartnerDetails.getBusinessType());
        businessPartner.setBusinessItem(businessPartnerDetails.getBusinessItem());
        businessPartner.setUseYn(businessPartnerDetails.getUseYn());

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
