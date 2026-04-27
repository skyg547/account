package com.ho.account.common.adapter;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.service.ExpenditureService;
import org.springframework.stereotype.Component;

@Component
public class MonolithLeasePaymentResolutionAdapter implements LeasePaymentResolutionPort {

    private final ExpenditureService expenditureService;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public MonolithLeasePaymentResolutionAdapter(ExpenditureService expenditureService,
                                                 DepartmentPersistencePort departmentPersistencePort,
                                                 AccountSubjectPersistencePort accountSubjectPersistencePort,
                                                 BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.expenditureService = expenditureService;
        this.departmentPersistencePort = departmentPersistencePort;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    @Override
    public void createLeasePaymentResolution(LeasePaymentResolutionCommand command) {
        Department department = departmentPersistencePort.findByCode(command.departmentCode())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + command.departmentCode()));
        AccountSubject accountSubject = accountSubjectPersistencePort.findByCode(command.accountCode())
                .orElseThrow(() -> new IllegalArgumentException("Account subject not found: " + command.accountCode()));
        BusinessPartner businessPartner = businessPartnerPersistencePort.findByBusinessPartnerCode(command.businessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("Business partner not found: " + command.businessPartnerCode()));

        ExpenditureResolution resolution = new ExpenditureResolution();
        resolution.setTitle(command.title());
        resolution.setResolutionDate(command.resolutionDate());
        resolution.setPaymentDate(command.paymentDate());
        resolution.setDepartment(department);

        ExpenditureDetail detail = new ExpenditureDetail();
        detail.setAccountSubject(accountSubject);
        detail.setAmount(command.amount());
        detail.setBusinessPartner(businessPartner);
        detail.setDescription(command.detailDescription());
        resolution.addDetail(detail);

        expenditureService.createResolution(resolution);
    }
}
