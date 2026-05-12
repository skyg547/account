package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;

/**
 * Application port that applies approved master-data change requests.
 */
public interface MasterDataChangeApplier {

    void apply(MasterDataChangeRequest request);
}
