package com.ho.account.contracts.masterdata;

import java.util.Optional;

public interface MasterDataQueryPort {

    Optional<AccountSubjectRef> findAccountSubject(String accountCode);

    Optional<BusinessPartnerRef> findBusinessPartner(String businessPartnerCode);

    Optional<DepartmentRef> findDepartment(String departmentCode);
}
