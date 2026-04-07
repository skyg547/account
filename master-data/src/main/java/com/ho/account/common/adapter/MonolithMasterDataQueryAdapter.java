package com.ho.account.common.adapter;

import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.BusinessPartnerRef;
import com.ho.account.contracts.masterdata.DepartmentRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MonolithMasterDataQueryAdapter implements MasterDataQueryPort {

    private final AccountSubjectRepository accountSubjectRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final DepartmentRepository departmentRepository;

    public MonolithMasterDataQueryAdapter(
            AccountSubjectRepository accountSubjectRepository,
            BusinessPartnerRepository businessPartnerRepository,
            DepartmentRepository departmentRepository) {
        this.accountSubjectRepository = accountSubjectRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public Optional<AccountSubjectRef> findAccountSubject(String accountCode) {
        return accountSubjectRepository.findByCode(accountCode)
                .map(account -> new AccountSubjectRef(
                        account.getCode(),
                        account.getName(),
                        account.isUnsettled(),
                        account.isFixedAsset()));
    }

    @Override
    public Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode) {
        return businessPartnerRepository.findByBusinessPartnerCode(businessPartnerCode)
                .map(partner -> new BusinessPartnerRef(
                        partner.getBusinessPartnerCode(),
                        partner.getBusinessPartnerName(),
                        partner.getPartnerType().name(),
                        partner.getUseYn()));
    }

    @Override
    public Optional<DepartmentRef> findDepartment(String departmentCode) {
        return departmentRepository.findByCode(departmentCode)
                .map(department -> new DepartmentRef(
                        department.getCode(),
                        department.getName(),
                        department.getType() != null ? department.getType().name() : null));
    }
}
