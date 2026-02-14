package com.ho.account.asset;

import com.ho.account.AccountApplication;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.asset.dto.LeaseContractRequest;
import com.ho.account.asset.dto.LeaseRemeasurementRequest;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.asset.repository.LeaseLiabilityRepository;
import com.ho.account.asset.repository.RightOfUseAssetRepository;
import com.ho.account.asset.service.LeaseAccountingService;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.repository.JournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = AccountApplication.class)
@Transactional
public class LeaseAccountingIntegrationTest {

    @Autowired
    private LeaseAccountingService leaseAccountingService;
    @Autowired
    private LeaseContractRepository leaseContractRepository;
    @Autowired
    private RightOfUseAssetRepository rightOfUseAssetRepository;
    @Autowired
    private LeaseLiabilityRepository leaseLiabilityRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;
    @Autowired
    private AccountSubjectRepository accountSubjectRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    private Department testDepartment;
    private BusinessPartner testLessor;
    private AccountSubject expenseAccount;
    private AccountSubject rouAssetAccount;
    private AccountSubject leaseLiabilityAccount;
    private AccountSubject depreciationExpenseAccount;
    private AccountSubject accumulatedDepreciationAccount;
    private AccountSubject interestExpenseAccount;
    private AccountSubject cashAccount;


    @BeforeEach
    void setUp() {
        // Mock data setup
        testDepartment = new Department();
        testDepartment.setCode("DEPT001");
        testDepartment.setName("테스트 부서");
        testDepartment.setUseYn(true);
        departmentRepository.save(testDepartment);

        testLessor = new BusinessPartner();
        testLessor.setBusinessPartnerCode("BP001");
        testLessor.setBusinessPartnerName("테스트 리스 제공자");
        testLessor.setUseYn(true);
        businessPartnerRepository.save(testLessor);

        expenseAccount = new AccountSubject();
        expenseAccount.setCode("61000"); // 예시 비용 계정
        expenseAccount.setName("지급임차료");
        expenseAccount.setUseYn(true);
        expenseAccount.setUnsettled(false);
        accountSubjectRepository.save(expenseAccount);

        rouAssetAccount = new AccountSubject();
        rouAssetAccount.setCode("12300"); // 사용권자산
        rouAssetAccount.setName("사용권자산");
        rouAssetAccount.setUseYn(true);
        rouAssetAccount.setUnsettled(false);
        accountSubjectRepository.save(rouAssetAccount);

        leaseLiabilityAccount = new AccountSubject();
        leaseLiabilityAccount.setCode("25100"); // 리스부채
        leaseLiabilityAccount.setName("리스부채");
        leaseLiabilityAccount.setUseYn(true);
        leaseLiabilityAccount.setUnsettled(false);
        accountSubjectRepository.save(leaseLiabilityAccount);

        depreciationExpenseAccount = new AccountSubject();
        depreciationExpenseAccount.setCode("51500"); // 감가상각비
        depreciationExpenseAccount.setName("감가상각비");
        depreciationExpenseAccount.setUseYn(true);
        depreciationExpenseAccount.setUnsettled(false);
        accountSubjectRepository.save(depreciationExpenseAccount);

        accumulatedDepreciationAccount = new AccountSubject();
        accumulatedDepreciationAccount.setCode("12399"); // 사용권자산 감가상각누계액
        accumulatedDepreciationAccount.setName("사용권자산 감가상각누계액");
        accumulatedDepreciationAccount.setUseYn(true);
        accumulatedDepreciationAccount.setUnsettled(false);
        accountSubjectRepository.save(accumulatedDepreciationAccount);

        interestExpenseAccount = new AccountSubject();
        interestExpenseAccount.setCode("93100"); // 이자비용
        interestExpenseAccount.setName("이자비용");
        interestExpenseAccount.setUseYn(true);
        interestExpenseAccount.setUnsettled(false);
        accountSubjectRepository.save(interestExpenseAccount);

        cashAccount = new AccountSubject();
        cashAccount.setCode("10100"); // 현금/예금
        cashAccount.setName("현금및현금성자산");
        cashAccount.setUseYn(true);
        cashAccount.setUnsettled(false);
        accountSubjectRepository.save(cashAccount);

        // Clear existing journal entries to ensure clean state for each test
        journalEntryRepository.deleteAll();
    }

    @Test
    @DisplayName("IFRS 16 리스 초기 인식 테스트 및 분개 검증")
    void testInitialLeaseRecognitionAndJournalEntry() {
        // Given
        LeaseContractRequest request = new LeaseContractRequest();
        request.setContractNo("LCS-001");
        request.setContractName("사무실 임대 리스");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2027, 12, 31)); // 3년
        request.setMonthlyPayment(new BigDecimal("1000000"));
        request.setPaymentDay(1);
        request.setLessorBusinessPartnerCode(testLessor.getBusinessPartnerCode());
        request.setDepartmentCode(testDepartment.getCode());
        request.setExpenseAccountCode(expenseAccount.getCode());
        request.setIfrs16Applicable(true);
        request.setDiscountRate(new BigDecimal("0.05")); // 5%
        request.setInitialRightOfUseAssetValue(new BigDecimal("33000000")); // 예시 값
        request.setInitialLeaseLiabilityValue(new BigDecimal("33000000")); // 예시 값

        LeaseContract contract = new LeaseContract();
        contract.setContractNo(request.getContractNo());
        contract.setContractName(request.getContractName());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setMonthlyPayment(request.getMonthlyPayment());
        contract.setPaymentDay(request.getPaymentDay());
        contract.setLessor(testLessor);
        contract.setDepartment(testDepartment);
        contract.setExpenseAccount(expenseAccount);
        contract.setIfrs16Applicable(request.isIfrs16Applicable());
        contract.setDiscountRate(request.getDiscountRate());
        contract.setInitialRightOfUseAssetValue(request.getInitialRightOfUseAssetValue());
        contract.setInitialLeaseLiabilityValue(request.getInitialLeaseLiabilityValue());


        // When
        LeaseContract recognizedContract = leaseAccountingService.recognizeInitialLease(contract);

        // Then
        assertThat(recognizedContract).isNotNull();
        assertThat(recognizedContract.getId()).isNotNull();

        Optional<RightOfUseAsset> rouAssetOpt = rightOfUseAssetRepository.findByLeaseContract(recognizedContract);
        assertThat(rouAssetOpt).isPresent();
        RightOfUseAsset rouAsset = rouAssetOpt.get();
        assertThat(rouAsset.getInitialValue()).isEqualByComparingTo(request.getInitialRightOfUseAssetValue());
        assertThat(rouAsset.getCurrentBookValue()).isEqualByComparingTo(request.getInitialRightOfUseAssetValue());

        Optional<LeaseLiability> leaseLiabilityOpt = leaseLiabilityRepository.findByLeaseContract(recognizedContract);
        assertThat(leaseLiabilityOpt).isPresent();
        LeaseLiability leaseLiability = leaseLiabilityOpt.get();
        assertThat(leaseLiability.getInitialValue()).isEqualByComparingTo(request.getInitialLeaseLiabilityValue());
        assertThat(leaseLiability.getCurrentValue()).isEqualByComparingTo(request.getInitialLeaseLiabilityValue());

        // 분개 검증
        List<JournalEntry> journalEntries = journalEntryRepository.findByLineageSourceTypeAndLineageSourceId("IFRS16_LEASE", recognizedContract.getId().toString());
        assertThat(journalEntries).hasSize(1);
        JournalEntry initialEntry = journalEntries.get(0);
        assertThat(initialEntry.getDescription()).contains("IFRS 16 리스 초기 인식");
        assertThat(initialEntry.getEntryType()).isEqualTo("IFRS16_INITIAL");
        assertThat(initialEntry.getDetails()).hasSize(2);

        // 차변: 사용권자산
        JournalDetail debitDetail = initialEntry.getDetails().stream().filter(d -> d.getDrcrType().equals("DEBIT")).findFirst().get();
        assertThat(debitDetail.getAccountSubject().getCode()).isEqualTo(rouAssetAccount.getCode());
        assertThat(debitDetail.getAmount()).isEqualByComparingTo(request.getInitialRightOfUseAssetValue());

        // 대변: 리스부채
        JournalDetail creditDetail = initialEntry.getDetails().stream().filter(d -> d.getDrcrType().equals("CREDIT")).findFirst().get();
        assertThat(creditDetail.getAccountSubject().getCode()).isEqualTo(leaseLiabilityAccount.getCode());
        assertThat(creditDetail.getAmount()).isEqualByComparingTo(request.getInitialLeaseLiabilityValue());
    }

    @Test
    @DisplayName("월별 리스 회계 처리 및 분개 검증")
    void testMonthlyLeaseProcessingAndJournalEntries() {
        // Given - 초기 인식된 리스 계약
        LeaseContract contract = createAndRecognizeLease(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), new BigDecimal("1000000"), new BigDecimal("10000000"), new BigDecimal("0.05"));
        Long contractId = contract.getId();

        RightOfUseAsset initialRouAsset = rightOfUseAssetRepository.findByLeaseContract(contract).get();
        LeaseLiability initialLeaseLiability = leaseLiabilityRepository.findByLeaseContract(contract).get();
        BigDecimal initialRouValue = initialRouAsset.getCurrentBookValue();
        BigDecimal initialLeaseLiabilityValue = initialLeaseLiability.getCurrentValue();

        // When - 1개월 후 처리
        LocalDate processDate1 = LocalDate.of(2025, 1, 1);
        leaseAccountingService.processMonthlyLeaseAccounting(processDate1);

        // Then - 상태 검증
        RightOfUseAsset updatedRouAsset1 = rightOfUseAssetRepository.findByLeaseContract(contract).get();
        LeaseLiability updatedLeaseLiability1 = leaseLiabilityRepository.findByLeaseContract(contract).get();

        assertThat(updatedRouAsset1.getCurrentBookValue()).isLessThan(initialRouValue);
        assertThat(updatedLeaseLiability1.getCurrentValue()).isLessThan(initialLeaseLiabilityValue);

        // 분개 검증 (2건: 상각, 상환)
        List<JournalEntry> monthlyEntries = journalEntryRepository.findByAccountingDateAndLineageSourceId(processDate1, contractId.toString());
        assertThat(monthlyEntries).hasSize(2);

        // 상각 분개 검증
        JournalEntry depreciationEntry = monthlyEntries.stream().filter(e -> e.getDescription().contains("감가상각")).findFirst().get();
        assertThat(depreciationEntry.getEntryType()).isEqualTo("IFRS16_MONTHLY");
        assertThat(depreciationEntry.getDetails()).hasSize(2);
        // 차변: 감가상각비
        JournalDetail depDebit = depreciationEntry.getDetails().stream().filter(d -> d.getAccountSubject().getCode().equals(depreciationExpenseAccount.getCode())).findFirst().get();
        assertThat(depDebit.getAmount()).isEqualByComparingTo(initialRouAsset.getDepreciationAmountPerPeriod());
        // 대변: 사용권자산 감가상각누계액
        JournalDetail depCredit = depreciationEntry.getDetails().stream().filter(d -> d.getAccountSubject().getCode().equals(accumulatedDepreciationAccount.getCode())).findFirst().get();
        assertThat(depCredit.getAmount()).isEqualByComparingTo(initialRouAsset.getDepreciationAmountPerPeriod());

        // 상환 분개 검증
        JournalEntry paymentEntry = monthlyEntries.stream().filter(e -> e.getDescription().contains("상환")).findFirst().get();
        assertThat(paymentEntry.getEntryType()).isEqualTo("IFRS16_MONTHLY");
        assertThat(paymentEntry.getDetails()).hasSize(3);
        // 차변: 이자비용, 리스부채
        List<JournalDetail> paymentDebits = paymentEntry.getDetails().stream().filter(d -> d.getDrcrType().equals("DEBIT")).toList();
        assertThat(paymentDebits).hasSize(2);
        // 대변: 현금/예금
        JournalDetail paymentCredit = paymentEntry.getDetails().stream().filter(d -> d.getDrcrType().equals("CREDIT")).findFirst().get();
        assertThat(paymentCredit.getAmount()).isEqualByComparingTo(contract.getMonthlyPayment());
    }

    @Test
    @DisplayName("리스 계약 재측정 및 분개 검증")
    void testLeaseRemeasurementAndJournalEntries() {
        // Given - 3개월 처리된 리스 계약
        LeaseContract contract = createAndRecognizeLease(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), new BigDecimal("1000000"), new BigDecimal("10000000"), new BigDecimal("0.05"));
        Long contractId = contract.getId();
        leaseAccountingService.processMonthlyLeaseAccounting(LocalDate.of(2025, 1, 1));
        leaseAccountingService.processMonthlyLeaseAccounting(LocalDate.of(2025, 2, 1));
        leaseAccountingService.processMonthlyLeaseAccounting(LocalDate.of(2025, 3, 1));

        RightOfUseAsset rouAssetBeforeRemeasurement = rightOfUseAssetRepository.findByLeaseContract(contract).get();
        LeaseLiability leaseLiabilityBeforeRemeasurement = leaseLiabilityRepository.findByLeaseContract(contract).get();
        BigDecimal oldRouValue = rouAssetBeforeRemeasurement.getCurrentBookValue();
        BigDecimal oldLeaseLiabilityValue = leaseLiabilityBeforeRemeasurement.getCurrentValue();

        // When - 리스 조건 변경 (월 리스료 증가, 종료일 연장, 할인율 변경)
        LocalDate remeasurementDate = LocalDate.of(2025, 4, 1);
        LeaseRemeasurementRequest remeasurementRequest = new LeaseRemeasurementRequest();
        remeasurementRequest.setContractId(contractId);
        remeasurementRequest.setRemeasurementDate(remeasurementDate);
        remeasurementRequest.setNewMonthlyPayment(new BigDecimal("1200000")); // 월 리스료 증가
        remeasurementRequest.setNewEndDate(LocalDate.of(2028, 12, 31)); // 종료일 연장
        remeasurementRequest.setNewDiscountRate(new BigDecimal("0.06")); // 할인율 변경 (6%)

        LeaseContract updatedContract = leaseAccountingService.remeasureLease(
                remeasurementRequest.getContractId(),
                remeasurementRequest.getRemeasurementDate(),
                remeasurementRequest.getNewMonthlyPayment(),
                remeasurementRequest.getNewEndDate(),
                remeasurementRequest.getNewDiscountRate()
        );

        // Then - 상태 검증
        RightOfUseAsset rouAssetAfterRemeasurement = rightOfUseAssetRepository.findByLeaseContract(updatedContract).get();
        LeaseLiability leaseLiabilityAfterRemeasurement = leaseLiabilityRepository.findByLeaseContract(updatedContract).get();

        assertThat(updatedContract.getMonthlyPayment()).isEqualByComparingTo(remeasurementRequest.getNewMonthlyPayment());
        assertThat(updatedContract.getEndDate()).isEqualTo(remeasurementRequest.getNewEndDate());
        assertThat(updatedContract.getDiscountRate()).isEqualByComparingTo(remeasurementRequest.getNewDiscountRate());

        assertThat(rouAssetAfterRemeasurement.getCurrentBookValue()).isNotEqualByComparingTo(oldRouValue);
        assertThat(leaseLiabilityAfterRemeasurement.getCurrentValue()).isNotEqualByComparingTo(oldLeaseLiabilityValue);
        assertThat(leaseLiabilityAfterRemeasurement.getCurrentValue()).isGreaterThan(oldLeaseLiabilityValue); // 리스료 증가, 기간 연장으로 부채 증가 예상

        // 분개 검증 (재측정 분개)
        List<JournalEntry> remeasurementEntries = journalEntryRepository.findByAccountingDateAndLineageSourceId(remeasurementDate, contractId.toString());
        assertThat(remeasurementEntries).hasSize(1);
        JournalEntry remeasurementEntry = remeasurementEntries.get(0);
        assertThat(remeasurementEntry.getDescription()).contains("IFRS 16 리스 재측정");
        assertThat(remeasurementEntry.getEntryType()).isEqualTo("IFRS16_REMEASUREMENT");
        assertThat(remeasurementEntry.getDetails()).hasSize(2);

        // 차변/대변 검증 (증가했으므로 사용권자산 차변, 리스부채 대변)
        JournalDetail remeasurementDebit = remeasurementEntry.getDetails().stream().filter(d -> d.getDrcrType().equals("DEBIT")).findFirst().get();
        JournalDetail remeasurementCredit = remeasurementEntry.getDetails().stream().filter(d -> d.getDrcrType().equals("CREDIT")).findFirst().get();

        assertThat(remeasurementDebit.getAccountSubject().getCode()).isEqualTo(rouAssetAccount.getCode());
        assertThat(remeasurementCredit.getAccountSubject().getCode()).isEqualTo(leaseLiabilityAccount.getCode());
        assertThat(remeasurementDebit.getAmount()).isEqualByComparingTo(remeasurementCredit.getAmount()); // 차대변 일치
    }


    private LeaseContract createAndRecognizeLease(LocalDate startDate, LocalDate endDate, BigDecimal monthlyPayment, BigDecimal initialValue, BigDecimal discountRate) {
        LeaseContract contract = new LeaseContract();
        contract.setContractNo("TEST-" + System.nanoTime()); // Unique contract no
        contract.setContractName("테스트 리스 계약");
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setMonthlyPayment(monthlyPayment);
        contract.setPaymentDay(1);
        contract.setLessor(testLessor);
        contract.setDepartment(testDepartment);
        contract.setExpenseAccount(expenseAccount);
        contract.setIfrs16Applicable(true);
        contract.setDiscountRate(discountRate);
        contract.setInitialRightOfUseAssetValue(initialValue);
        contract.setInitialLeaseLiabilityValue(initialValue);
        return leaseAccountingService.recognizeInitialLease(contract);
    }
}
