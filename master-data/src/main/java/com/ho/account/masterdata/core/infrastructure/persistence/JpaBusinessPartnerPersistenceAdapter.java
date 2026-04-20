package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.masterdata.core.port.out.BusinessPartnerPersistencePort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaBusinessPartnerPersistenceAdapter implements BusinessPartnerPersistencePort {

    private final BusinessPartnerRepository businessPartnerRepository;

    public JpaBusinessPartnerPersistenceAdapter(BusinessPartnerRepository businessPartnerRepository) {
        this.businessPartnerRepository = businessPartnerRepository;
    }

    @Override
    public boolean existsByBusinessPartnerCode(String businessPartnerCode) {
        return businessPartnerRepository.existsByBusinessPartnerCode(businessPartnerCode);
    }

    @Override
    public Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode) {
        return businessPartnerRepository.findByBusinessPartnerCode(businessPartnerCode);
    }

    @Override
    public Optional<BusinessPartner> findById(Long id) {
        return businessPartnerRepository.findById(id);
    }

    @Override
    public List<BusinessPartner> findAll() {
        return businessPartnerRepository.findAll();
    }

    @Override
    public List<BusinessPartner> findByUseYnTrue() {
        return businessPartnerRepository.findByUseYnTrue();
    }

    @Override
    public List<BusinessPartner> findByBusinessPartnerNameContaining(String name) {
        return businessPartnerRepository.findByBusinessPartnerNameContaining(name);
    }

    @Override
    public BusinessPartner save(BusinessPartner businessPartner) {
        return businessPartnerRepository.save(businessPartner);
    }
}
