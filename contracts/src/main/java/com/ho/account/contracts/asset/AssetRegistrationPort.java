package com.ho.account.contracts.asset;

public interface AssetRegistrationPort {

    void registerAcquiredAsset(AssetAcquisitionCommand command);

    void activateLeaseContract(Long leaseContractId);
}
