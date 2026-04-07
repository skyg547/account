package com.ho.account.asset.service;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LeaseService {

    private final LeaseContractRepository leaseContractRepository;
    private final LeasePaymentResolutionPort leasePaymentResolutionPort;
    private final LeaseAccountingService leaseAccountingService;

    public LeaseService(LeaseContractRepository leaseContractRepository,
                        LeasePaymentResolutionPort leasePaymentResolutionPort,
                        LeaseAccountingService leaseAccountingService) {
        this.leaseContractRepository = leaseContractRepository;
        this.leasePaymentResolutionPort = leasePaymentResolutionPort;
        this.leaseAccountingService = leaseAccountingService;
    }

    public LeaseContract registerContract(LeaseContract contract) {
        LeaseContract savedContract = leaseContractRepository.save(contract);

        if (savedContract.isIfrs16Applicable() && !savedContract.isShortTermLease() && !savedContract.isLowValueLease()) {
            leaseAccountingService.recognizeInitialLease(savedContract);
        }
        return savedContract;
    }

    public void processMonthlyLeasePayment(LocalDate paymentDate) {
        List<LeaseContract> activeContracts = leaseContractRepository.findByStatus("ACTIVE");

        for (LeaseContract contract : activeContracts) {
            if (contract.getPaymentDay() == paymentDate.getDayOfMonth()) {
                createLeaseExpenditure(contract, paymentDate);
            }
        }
    }

    private void createLeaseExpenditure(LeaseContract contract, LocalDate date) {
        leasePaymentResolutionPort.createLeasePaymentResolution(new LeasePaymentResolutionCommand(
                "리스료 지급 " + contract.getContractName(),
                date,
                date,
                contract.getDepartment().getCode(),
                contract.getExpenseAccount().getCode(),
                contract.getLessor().getBusinessPartnerCode(),
                contract.getMonthlyPayment(),
                "월 리스료"
        ));
    }
}
