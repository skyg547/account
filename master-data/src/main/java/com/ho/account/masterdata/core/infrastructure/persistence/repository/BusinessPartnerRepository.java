package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessPartnerRepository extends JpaRepository<BusinessPartner, Long> {
    Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode);
    List<BusinessPartner> findByUseYnTrue();
    boolean existsByBusinessPartnerCode(String businessPartnerCode);
    List<BusinessPartner> findByBusinessPartnerNameContaining(String name);
}
