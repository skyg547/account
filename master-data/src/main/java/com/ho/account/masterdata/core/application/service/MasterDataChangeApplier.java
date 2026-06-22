package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;

/**
 * Application port that applies approved master-data change requests.
 */
public interface MasterDataChangeApplier {

    boolean supports(MasterDataChangeRequest.MasterDataType targetType);

    void apply(MasterDataChangeRequest request);
}
