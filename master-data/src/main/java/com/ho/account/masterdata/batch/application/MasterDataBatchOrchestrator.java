package com.ho.account.masterdata.batch.application;

import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.port.out.ProductPersistencePort;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class MasterDataBatchOrchestrator {

    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final ProductPersistencePort productPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public MasterDataBatchOrchestrator(
            AccountSubjectPersistencePort accountSubjectPersistencePort,
            DepartmentPersistencePort departmentPersistencePort,
            ProductPersistencePort productPersistencePort,
            BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.productPersistencePort = productPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    public MasterDataBatchReport createDailyValidityReport(LocalDate asOfDate) {
        long activeAccountSubjects = accountSubjectPersistencePort.findAll().stream()
                .filter(account -> MasterDataValidityPolicy.isActiveAt(asOfDate, account.getValidFrom(), account.getValidTo()))
                .count();
        long activeDepartments = departmentPersistencePort.findAll().stream()
                .filter(department -> MasterDataValidityPolicy.isActiveAt(asOfDate, department.getValidFrom(), department.getValidTo()))
                .count();
        long activeProducts = productPersistencePort.findAll().stream()
                .filter(product -> MasterDataValidityPolicy.isActiveAt(asOfDate, product.getValidFrom(), product.getValidTo()))
                .count();
        long activeBusinessPartners = businessPartnerPersistencePort.findByUseYnTrue().stream().count();

        return new MasterDataBatchReport(
                asOfDate,
                activeAccountSubjects,
                activeDepartments,
                activeProducts,
                activeBusinessPartners);
    }
}
