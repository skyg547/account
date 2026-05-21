package com.ho.account.reporting.application.port.out;

import com.ho.account.reporting.domain.model.RegulatoryFiling;

public interface StoreRegulatoryFilingPort {

    void save(RegulatoryFiling filing);
}
