package com.ho.account.asset.infrastructure.persistence;

import com.ho.account.asset.application.port.out.LeasePersistencePort;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.asset.repository.LeaseLiabilityRepository;
import com.ho.account.asset.repository.LeasePaymentScheduleRepository;
import com.ho.account.asset.repository.RightOfUseAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeasePersistenceAdapter implements LeasePersistencePort {

    private final LeaseContractRepository leaseContractRepository;
    private final RightOfUseAssetRepository rightOfUseAssetRepository;
    private final LeaseLiabilityRepository leaseLiabilityRepository;
    private final LeasePaymentScheduleRepository leasePaymentScheduleRepository;

    @Override
    public LeaseContract saveContract(LeaseContract contract) {
        return leaseContractRepository.save(contract);
    }

    @Override
    public Optional<LeaseContract> findContractById(Long id) {
        return leaseContractRepository.findById(id);
    }

    @Override
    public List<LeaseContract> findActiveContracts(String status) {
        return leaseContractRepository.findByStatus(status);
    }

    @Override
    public List<LeaseContract> findIfrs16ApplicableActiveContracts() {
        return leaseContractRepository.findByIfrs16ApplicableTrueAndStatus("ACTIVE");
    }

    @Override
    public RightOfUseAsset saveROUAsset(RightOfUseAsset asset) {
        return rightOfUseAssetRepository.save(asset);
    }

    @Override
    public Optional<RightOfUseAsset> findROUAssetByContract(LeaseContract contract) {
        return rightOfUseAssetRepository.findByLeaseContract(contract);
    }

    @Override
    public LeaseLiability saveLiability(LeaseLiability liability) {
        return leaseLiabilityRepository.save(liability);
    }

    @Override
    public Optional<LeaseLiability> findLiabilityByContract(LeaseContract contract) {
        return leaseLiabilityRepository.findByLeaseContract(contract);
    }

    @Override
    public void savePaymentSchedules(List<LeasePaymentSchedule> schedules) {
        leasePaymentScheduleRepository.saveAll(schedules);
    }

    @Override
    public void savePaymentSchedule(LeasePaymentSchedule schedule) {
        leasePaymentScheduleRepository.save(schedule);
    }

    @Override
    public List<LeasePaymentSchedule> findSchedulesByContract(LeaseContract contract) {
        return leasePaymentScheduleRepository.findByLeaseContractOrderByPaymentDateAsc(contract);
    }
}
