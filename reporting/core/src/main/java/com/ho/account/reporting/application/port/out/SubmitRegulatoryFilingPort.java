package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.RegulatoryFilingPackage;
import com.ho.account.reporting.domain.model.RegulatoryFilingReceipt;

public interface SubmitRegulatoryFilingPort {

    RegulatoryFilingReceipt submit(RegulatoryFilingPackage filingPackage);
}
