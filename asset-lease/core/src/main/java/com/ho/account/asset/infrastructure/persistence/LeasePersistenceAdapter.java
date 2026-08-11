package com.ho.account.asset.infrastructure.persistence;

import com.ho.account.asset.application.port.out.LeasePersistencePort;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.asset.infrastructure.persistence.repository.LeaseContractRepository;
import com.ho.account.asset.infrastructure.persistence.repository.LeaseLiabilityRepository;
import com.ho.account.asset.infrastructure.persistence.repository.LeasePaymentScheduleRepository;
import com.ho.account.asset.infrastructure.persistence.repository.RightOfUseAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 영속성 아웃바운드 어댑터 (Persistence Outbound Adapter)]
 * LeasePersistencePort를 구현하여 리스 계약 관련 엔티티의 영속성을 처리하는 인프라 어댑터입니다.
 * 
 * 💡 [교육적 주석 - 헥사고날/DIP 이점]
 * 리스 관련 JPA Repositories(LeaseContractRepository 등)를 인프라 레이어 어댑터 내부에 은닉하고,
 * 도메인과 서비스는 LeasePersistencePort 인터페이스를 통해 접근하도록 격리합니다.
 */
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
