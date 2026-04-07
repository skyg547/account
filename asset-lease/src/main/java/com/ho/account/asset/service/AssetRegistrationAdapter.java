package com.ho.account.asset.service;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.contracts.asset.AssetAcquisitionCommand;
import com.ho.account.contracts.asset.AssetRegistrationPort;
import org.springframework.stereotype.Component;

@Component
public class AssetRegistrationAdapter implements AssetRegistrationPort {

    private final FixedAssetService fixedAssetService;
    private final LeaseContractRepository leaseContractRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;

    public AssetRegistrationAdapter(FixedAssetService fixedAssetService,
                                    LeaseContractRepository leaseContractRepository,
                                    AccountSubjectRepository accountSubjectRepository,
                                    DepartmentRepository departmentRepository) {
        this.fixedAssetService = fixedAssetService;
        this.leaseContractRepository = leaseContractRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public void registerAcquiredAsset(AssetAcquisitionCommand command) {
        FixedAsset asset = new FixedAsset();
        asset.setAssetCode(command.assetCode());
        asset.setAssetName(command.assetName());
        asset.setAccountSubject(accountSubjectRepository.findByCode(command.accountCode())
                .orElseThrow(() -> new IllegalArgumentException("Account subject not found: " + command.accountCode())));
        asset.setAcquisitionDate(command.acquisitionDate());
        asset.setAcquisitionCost(command.acquisitionCost());
        asset.setDepartment(departmentRepository.findByCode(command.departmentCode())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + command.departmentCode())));
        asset.setUsefulLife(command.usefulLife());
        asset.setDepreciationMethod(command.depreciationMethod());
        asset.setStatus(command.status());
        fixedAssetService.registerAsset(asset);
    }

    @Override
    public void activateLeaseContract(Long leaseContractId) {
        leaseContractRepository.findById(leaseContractId).ifPresent(contract -> {
            if (!"ACTIVE".equals(contract.getStatus())) {
                contract.setStatus("ACTIVE");
                leaseContractRepository.save(contract);
            }
        });
    }
}
