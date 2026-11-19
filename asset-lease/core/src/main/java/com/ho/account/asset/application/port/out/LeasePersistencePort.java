package com.ho.account.asset.application.port.out;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseRemeasurementReceipt;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import java.util.List;
import java.util.Optional;

public interface LeasePersistencePort {
    LeaseContract saveContract(LeaseContract contract);
    Optional<LeaseContract> findContractByNumber(String contractNo);
    Optional<LeaseRemeasurementReceipt> findRemeasurementReceipt(String contractNo, String operationId);
    void saveRemeasurementReceipt(LeaseRemeasurementReceipt receipt);
    Optional<LeaseContract> findContractById(Long id);
    Optional<LeaseContract> findContractByIdForUpdate(Long id);
    List<LeaseContract> findActiveContracts(String status);
    List<LeaseContract> findIfrs16ApplicableActiveContracts();

    RightOfUseAsset saveROUAsset(RightOfUseAsset asset);
    Optional<RightOfUseAsset> findROUAssetByContract(LeaseContract contract);

    LeaseLiability saveLiability(LeaseLiability liability);
    Optional<LeaseLiability> findLiabilityByContract(LeaseContract contract);

    void savePaymentSchedules(List<LeasePaymentSchedule> schedules);
    void savePaymentSchedule(LeasePaymentSchedule schedule);
    List<LeasePaymentSchedule> findSchedulesByContract(LeaseContract contract);
}
