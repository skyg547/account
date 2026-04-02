package com.ho.account.asset.service;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.expenditure.domain.ExpenditureDetail;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.service.ExpenditureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import com.ho.account.journal.service.JournalService; // 기존 JournalEntryService에서 변경
import java.util.List;

@Service
@Transactional
public class LeaseService {

    private final LeaseContractRepository leaseContractRepository;
    private final ExpenditureService expenditureService;
    private final LeaseAccountingService leaseAccountingService;
    private final JournalService journalService; // 기존 JournalEntryService에서 변경

    @Autowired
    public LeaseService(LeaseContractRepository leaseContractRepository,
                        ExpenditureService expenditureService,
                        LeaseAccountingService leaseAccountingService,
                        JournalService journalService) { // 기존 JournalEntryService에서 변경
        this.leaseContractRepository = leaseContractRepository;
        this.expenditureService = expenditureService;
        this.leaseAccountingService = leaseAccountingService;
        this.journalService = journalService; // 기존 JournalEntryService에서 변경
    }

    /**
     * 리스 계약을 등록하고, IFRS 16 적용 대상인 경우 초기 인식을 수행합니다.
     *
     * @param contract 등록할 리스 계약 엔티티
     * @return 등록 및 초기 인식 처리된 리스 계약 엔티티
     */
    public LeaseContract registerContract(LeaseContract contract) {
        LeaseContract savedContract = leaseContractRepository.save(contract);

        if (savedContract.isIfrs16Applicable() && !savedContract.isShortTermLease() && !savedContract.isLowValueLease()) {
            leaseAccountingService.recognizeInitialLease(savedContract);
        }
        return savedContract;
    }

    // 월 리스료 지급 결의서 자동 생성 (배치 작업용)
    public void processMonthlyLeasePayment(LocalDate paymentDate) {
        List<LeaseContract> activeContracts = leaseContractRepository.findByStatus("ACTIVE");

        for (LeaseContract contract : activeContracts) {
            // 지급일 체크 (해당 월의 지급일인지)
            if (contract.getPaymentDay() == paymentDate.getDayOfMonth()) {
                createLeaseExpenditure(contract, paymentDate);
            }
        }
    }

    // 지출 결의서 생성 로직
    private void createLeaseExpenditure(LeaseContract contract, LocalDate date) {
        ExpenditureResolution resolution = new ExpenditureResolution();
        resolution.setTitle("리스료 지급: " + contract.getContractName());
        resolution.setResolutionDate(date);
        resolution.setPaymentDate(date);
        resolution.setDepartment(contract.getDepartment());
        // 지급 계좌는 별도 설정 필요 (여기서는 생략 또는 기본값 사용)
        // resolution.setPaymentAccount(...);

        ExpenditureDetail detail = new ExpenditureDetail();
        detail.setAccountSubject(contract.getExpenseAccount());
        detail.setAmount(contract.getMonthlyPayment());
        detail.setBusinessPartner(contract.getLessor());
        detail.setDescription("월 리스료");

        resolution.addDetail(detail);

        // 결의서 생성 (DRAFT 상태)
        expenditureService.createResolution(resolution);
    }
}
