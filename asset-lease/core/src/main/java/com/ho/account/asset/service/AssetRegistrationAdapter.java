package com.ho.account.asset.service;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetRegistrationAdapter {

    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;

    public void validateAssetData(FixedAsset asset) {
        if (asset.getAccountSubject() == null || accountSubjectPersistencePort.findByCode(asset.getAccountSubject().getCode()).isEmpty()) {
            throw new IllegalArgumentException("Invalid Account Subject");
        }
        if (asset.getDepartment() == null || departmentPersistencePort.findByCode(asset.getDepartment().getCode()).isEmpty()) {
            throw new IllegalArgumentException("Invalid Department");
        }
    }
}
