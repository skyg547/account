package com.ho.account.common.adapter;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.service.ExpenditureService;
import org.springframework.stereotype.Component;

@Component
public class MonolithLeasePaymentResolutionAdapter implements LeasePaymentResolutionPort {

    private final ExpenditureService expenditureService;
    private final DepartmentRepository departmentRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final BusinessPartnerRepository businessPartnerRepository;

    public MonolithLeasePaymentResolutionAdapter(ExpenditureService expenditureService,
                                                 DepartmentRepository departmentRepository,
                                                 AccountSubjectRepository accountSubjectRepository,
                                                 BusinessPartnerRepository businessPartnerRepository) {
        this.expenditureService = expenditureService;
        this.departmentRepository = departmentRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    @Override
    public void createLeasePaymentResolution(LeasePaymentResolutionCommand command) {
        Department department = departmentRepository.findByCode(command.departmentCode())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + command.departmentCode()));
        AccountSubject accountSubject = accountSubjectRepository.findById(command.accountCode())
                .orElseThrow(() -> new IllegalArgumentException("Account subject not found: " + command.accountCode()));
        BusinessPartner businessPartner = businessPartnerRepository.findByBusinessPartnerCode(command.businessPartnerCode())
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
