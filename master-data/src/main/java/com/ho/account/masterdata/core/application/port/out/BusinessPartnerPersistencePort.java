package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Output port for business-partner persistence.
 *
 * <p>AP, AR, treasury, and other modules resolve counterparty reference data
 * through this application-layer contract instead of repository details.</p>
 */
public interface BusinessPartnerPersistencePort {

    boolean existsByBusinessPartnerCode(String businessPartnerCode);

    Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode);

    Optional<BusinessPartner> findById(Long id);

    List<BusinessPartner> findAll();

    List<BusinessPartner> findByUseYnTrue();

    List<BusinessPartner> searchActiveByName(String name, LocalDate asOfDate);

    BusinessPartner save(BusinessPartner businessPartner);
}
