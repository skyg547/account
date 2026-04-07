package com.ho.account.basic.service;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BusinessPartnerService {

    private final BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    public BusinessPartnerService(BusinessPartnerRepository businessPartnerRepository) {
        this.businessPartnerRepository = businessPartnerRepository;
    }

    // 거래처 생성
    public BusinessPartner createBusinessPartner(BusinessPartner businessPartner) {
        if (businessPartnerRepository.existsByBusinessPartnerCode(businessPartner.getBusinessPartnerCode())) {
            throw new IllegalArgumentException("이미 존재하는 거래처 코드입니다: " + businessPartner.getBusinessPartnerCode());
        }
        return businessPartnerRepository.save(businessPartner);
    }

    // 전체 거래처 조회
    @Transactional(readOnly = true)
    public List<BusinessPartner> getAllBusinessPartners() {
        return businessPartnerRepository.findAll();
    }

    // 사용 중인 거래처만 조회
    @Transactional(readOnly = true)
    public List<BusinessPartner> getActiveBusinessPartners() {
        return businessPartnerRepository.findByUseYnTrue();
    }

    // 거래처 상세 조회 (코드)
    @Transactional(readOnly = true)
    public Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode) {
        return businessPartnerRepository.findByBusinessPartnerCode(businessPartnerCode);
    }

    // 거래처 검색 (이름)
    @Transactional(readOnly = true)
    public List<BusinessPartner> searchBusinessPartnersByName(String name) {
        return businessPartnerRepository.findByBusinessPartnerNameContaining(name);
    }

    // 거래처 정보 수정
    public BusinessPartner updateBusinessPartner(Long id, BusinessPartner businessPartnerDetails) {
        BusinessPartner businessPartner = businessPartnerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));

        businessPartner.setBusinessPartnerName(businessPartnerDetails.getBusinessPartnerName());
        businessPartner.setRegistrationNumber(businessPartnerDetails.getRegistrationNumber());
        businessPartner.setCeoName(businessPartnerDetails.getCeoName());
        businessPartner.setBusinessType(businessPartnerDetails.getBusinessType());
        businessPartner.setBusinessItem(businessPartnerDetails.getBusinessItem());
        businessPartner.setUseYn(businessPartnerDetails.getUseYn());

        return businessPartnerRepository.save(businessPartner);
    }

    // 거래처 삭제 (논리적 삭제)
    public void deleteBusinessPartner(Long id) {
        BusinessPartner businessPartner = businessPartnerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래처를 찾을 수 없습니다. ID: " + id));
        businessPartner.setUseYn(false);
        businessPartnerRepository.save(businessPartner);
    }
}
