package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningDecision;
import com.ho.account.masterdata.core.application.command.BusinessPartnerScreeningSubmission;
import com.ho.account.masterdata.core.domain.model.BusinessPartnerScreening;

import java.time.LocalDate;

public interface BusinessPartnerScreeningUseCase {

    BusinessPartnerScreening submitScreening(BusinessPartnerScreeningSubmission submission);

    BusinessPartnerScreening decideScreening(BusinessPartnerScreeningDecision decision);

    boolean isEligible(String businessPartnerCode, LocalDate asOfDate);
}
