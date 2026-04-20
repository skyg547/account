package com.ho.account.masterdata.core.application.usecase;

import com.ho.account.basic.domain.BusinessPartner;
import java.util.List;
import java.util.Optional;

public interface BusinessPartnerUseCase {

    BusinessPartner createBusinessPartner(BusinessPartner businessPartner);

    List<BusinessPartner> getAllBusinessPartners();

    List<BusinessPartner> getActiveBusinessPartners();

    Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode);

    List<BusinessPartner> searchBusinessPartnersByName(String name);

    BusinessPartner updateBusinessPartner(Long id, BusinessPartner businessPartnerDetails);

    void deleteBusinessPartner(Long id);
}
