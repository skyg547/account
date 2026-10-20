package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.BusinessPartnerScreening;

import java.time.LocalDate;
import java.util.Optional;

/** Current screening state and immutable audit snapshots must share one transaction. */
public interface BusinessPartnerScreeningPort {

    Optional<BusinessPartnerScreening> findByBusinessPartnerCode(String code);

    /** Last immutable screening event through the given calendar date, including pending resubmissions. */
    Optional<BusinessPartnerScreening> findLatestAt(String businessPartnerCode, LocalDate asOfDate);

    Optional<BusinessPartnerScreening> findByBusinessPartnerCodeForUpdate(String code);

    BusinessPartnerScreening save(BusinessPartnerScreening screening);

    void appendAudit(BusinessPartnerScreening screening);
}
