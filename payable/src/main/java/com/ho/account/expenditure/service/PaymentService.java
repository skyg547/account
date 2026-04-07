package com.ho.account.expenditure.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.expenditure.domain.*;
import com.ho.account.expenditure.repository.AdvancePaymentRepository;
import com.ho.account.expenditure.repository.PayableRepository;
import com.ho.account.expenditure.repository.PaymentRepository;
import com.ho.account.expenditure.repository.PaymentRunRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.service.JournalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PayableRepository payableRepository;
    private final PaymentRunRepository paymentRunRepository;
    private final AdvancePaymentRepository advancePaymentRepository;
    private final BusinessPartnerRepository businessPartnerRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final JournalService journalService;

    public PaymentService(PaymentRepository paymentRepository,
                          PayableRepository payableRepository,
                          PaymentRunRepository paymentRunRepository,
                          AdvancePaymentRepository advancePaymentRepository,
                          BusinessPartnerRepository businessPartnerRepository,
                          AccountSubjectRepository accountSubjectRepository,
                          DepartmentRepository departmentRepository,
                          JournalService journalService) {
        this.paymentRepository = paymentRepository;
        this.payableRepository = payableRepository;
        this.paymentRunRepository = paymentRunRepository;
        this.advancePaymentRepository = advancePaymentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.journalService = journalService;
    }

    /**
     * 지급 실행을 시작하고, 만기일이 도래한 매입채무를 대상으로 지급을 생성합니다.
     * @param runDate 지급 실행 기준일
     * @param description 지급 실행 설명
     * @param createdBy 생성자
     * @return 생성된 지급 실행 정보
     */
    public PaymentRun initiatePaymentRun(LocalDate runDate, String description, String createdBy) {
        PaymentRun paymentRun = new PaymentRun();
        paymentRun.setRunDate(runDate);
        paymentRun.setDescription(description);
        paymentRun.setCreatedBy(createdBy);
        paymentRun.setStatus(PaymentRunStatus.INITIATED);
        PaymentRun savedPaymentRun = paymentRunRepository.save(paymentRun);

        // 만기일이 도래한 OPEN 상태의 매입채무 조회
        List<Payable> duePayables = payableRepository.findByDueDateBeforeAndStatusNot(runDate.plusDays(1), PayableStatus.PAID);

        for (Payable payable : duePayables) {
            // 해당 매입채무에 대한 지급 생성
            Payment payment = new Payment();
            payment.setPaymentDate(runDate);
            payment.setVendor(payable.getVendor());
            payment.setAmount(payable.getOutstandingAmount()); // 전액 지급 가정
            payment.setStatus(PaymentStatus.INITIATED);
            payment.setPaymentRun(savedPaymentRun);
            // TODO: 은행 계좌, 참조 번호 등은 지급 정책에 따라 결정

            paymentRepository.save(payment);
        }

        savedPaymentRun.setStatus(PaymentRunStatus.PROCESSING);
        return paymentRunRepository.save(savedPaymentRun);
    }

    /**
     * 지급을 실행하고 관련 전표를 생성합니다. (승인된 지급 건에 대해)
     * @param paymentId 지급 ID
     * @param bankAccount 지급 나갈 계좌 정보
     * @return 완료된 지급 정보
     */
    public Payment executePayment(Long paymentId, String bankAccount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("지급 정보를 찾을 수 없습니다: " + paymentId));

        if (payment.getStatus() != PaymentStatus.INITIATED && payment.getStatus() != PaymentStatus.APPROVED) {
            throw new IllegalStateException("지급 실행 가능한 상태가 아닙니다: " + payment.getStatus());
        }

        // TODO: 실제 은행 시스템 연동 로직
        boolean paymentSuccess = true; // 실제로는 외부 시스템 호출 결과

        if (paymentSuccess) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setBankAccount(bankAccount);
            Payment completedPayment = paymentRepository.save(payment);

            // 1. 매입채무 업데이트 및 전표 생성
            Payable payable = payableRepository.findByVendorBusinessPartnerCodeAndOutstandingAmountGreaterThan(payment.getVendor().getBusinessPartnerCode(), BigDecimal.ZERO)
                    .stream()
                    .filter(p -> p.getOutstandingAmount().compareTo(payment.getAmount()) >= 0) // 현재 지급액으로 커버 가능한 채무
                    .findFirst() // 가장 오래된 채무부터 지급 (또는 다른 로직)
                    .orElseThrow(() -> new IllegalStateException("지급할 매입채무를 찾을 수 없습니다."));

            // 매입채무 잔액 업데이트
            payable.setOutstandingAmount(payable.getOutstandingAmount().subtract(payment.getAmount()));
            if (payable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                payable.setStatus(PayableStatus.PAID);
                payable.setOutstandingAmount(BigDecimal.ZERO);
            } else {
                payable.setStatus(PayableStatus.PARTIAL_PAID);
            }
            payableRepository.save(payable);

            // 2. 지급 전표 생성
            AccountSubject apAccount = accountSubjectRepository.findById("21100") // 매입채무 계정 코드 (예시)
                    .orElseThrow(() -> new IllegalStateException("AccountSubject for Accounts Payable not found"));
            AccountSubject cashAccount = accountSubjectRepository.findById("10100") // 현금 또는 예금 계정 코드 (예시)
                    .orElseThrow(() -> new IllegalStateException("AccountSubject for Cash/Bank not found"));
            Department defaultDepartment = getOrCreateDefaultDepartment();

            JournalEntry paymentEntry = new JournalEntry();
            paymentEntry.setSlipDate(payment.getPaymentDate());
            paymentEntry.setAccountingDate(payment.getPaymentDate());
            paymentEntry.setDescription("지급 실행: " + payment.getVendor().getBusinessPartnerName() + " - " + payment.getAmount());
            paymentEntry.setEntryType("PAYMENT_EXECUTION");
            paymentEntry.setLineageSourceType("PAYMENT");
            paymentEntry.setLineageSourceId(completedPayment.getId().toString());
            paymentEntry.setCreatedBy("PAYMENT_MAKER");
            paymentEntry.setAuditUser("PAYMENT_CHECKER");
            paymentEntry.setStatus(JournalEntryStatus.DRAFT);

            // 차변: 매입채무 감소
            JournalDetail debitAp = new JournalDetail();
            debitAp.setDrcrType("DEBIT");
            debitAp.setAccountSubject(apAccount);
            debitAp.setAmount(payment.getAmount());
            debitAp.setDepartment(defaultDepartment);
            debitAp.setDetailDescription("매입채무 감소");
            paymentEntry.addDetail(debitAp);

            // 대변: 현금/예금 감소
            JournalDetail creditCash = new JournalDetail();
            creditCash.setDrcrType("CREDIT");
            creditCash.setAccountSubject(cashAccount);
            creditCash.setAmount(payment.getAmount());
            creditCash.setDetailDescription("현금/예금 감소");
            paymentEntry.addDetail(creditCash);

            journalService.createJournalEntry(paymentEntry);
            journalService.requestApproval(paymentEntry.getId()); // 승인 요청
            journalService.approveJournalEntry(paymentEntry.getId()); // 바로 승인 (테스트용)

            return completedPayment;
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("지급 실행에 실패했습니다.");
        }
    }


    /**
     * 선급금을 기록하고 전표를 생성합니다.
     * @param advancePayment 선급금 정보
     * @return 생성된 선급금 정보
     */
    public AdvancePayment recordAdvancePayment(AdvancePayment advancePayment) {
        BusinessPartner vendor = businessPartnerRepository.findByBusinessPartnerCode(advancePayment.getVendor().getBusinessPartnerCode())
                .orElseThrow(() -> new IllegalArgumentException("공급업체 정보를 찾을 수 없습니다: " + advancePayment.getVendor().getBusinessPartnerCode()));
        advancePayment.setVendor(vendor);

        AdvancePayment savedAdvancePayment = advancePaymentRepository.save(advancePayment);

        // 선급금 지급 전표: (차) 선급금 (대) 현금/예금
        AccountSubject advancePaymentAccount = accountSubjectRepository.findById("13100") // 선급금 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Advance Payment not found"));
        AccountSubject cashAccount = accountSubjectRepository.findById("10100") // 현금 또는 예금 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Cash/Bank not found"));
        Department defaultDepartment = getOrCreateDefaultDepartment();

        JournalEntry apEntry = new JournalEntry();
        apEntry.setSlipDate(advancePayment.getPaymentDate());
        apEntry.setAccountingDate(advancePayment.getPaymentDate());
        apEntry.setDescription("선급금 지급: " + advancePayment.getVendor().getBusinessPartnerName() + " - " + advancePayment.getAmount());
        apEntry.setEntryType("ADVANCE_PAYMENT");
        apEntry.setLineageSourceType("ADVANCE_PAYMENT");
        apEntry.setLineageSourceId(savedAdvancePayment.getId().toString());
        apEntry.setCreatedBy("PAYMENT_MAKER");
        apEntry.setAuditUser("PAYMENT_CHECKER");
        apEntry.setStatus(JournalEntryStatus.DRAFT);

        // 차변: 선급금
        JournalDetail debitAp = new JournalDetail();
        debitAp.setDrcrType("DEBIT");
        debitAp.setAccountSubject(advancePaymentAccount);
        debitAp.setAmount(advancePayment.getAmount());
        debitAp.setDepartment(defaultDepartment);
        debitAp.setDetailDescription("선급금 발생");
        apEntry.addDetail(debitAp);

        // 대변: 현금/예금
        JournalDetail creditCash = new JournalDetail();
        creditCash.setDrcrType("CREDIT");
        creditCash.setAccountSubject(cashAccount);
        creditCash.setAmount(advancePayment.getAmount());
        creditCash.setDetailDescription("현금/예금 감소");
        apEntry.addDetail(creditCash);

        journalService.createJournalEntry(apEntry);
        journalService.requestApproval(apEntry.getId());
        journalService.approveJournalEntry(apEntry.getId());

        return savedAdvancePayment;
    }

    /**
     * 매입채무를 선급금과 상계 처리합니다.
     * @param payableId 상계할 매입채무 ID
     * @param advancePaymentId 상계에 사용할 선급금 ID
     * @param offsetAmount 상계할 금액
     * @return 업데이트된 매입채무
     */
    public Payable offsetPayableWithAdvancePayment(Long payableId, Long advancePaymentId, BigDecimal offsetAmount) {
        Payable payable = payableRepository.findById(payableId)
                .orElseThrow(() -> new IllegalArgumentException("매입채무를 찾을 수 없습니다: " + payableId));
        AdvancePayment advancePayment = advancePaymentRepository.findById(advancePaymentId)
                .orElseThrow(() -> new IllegalArgumentException("선급금을 찾을 수 없습니다: " + advancePaymentId));

        if (offsetAmount.compareTo(BigDecimal.ZERO) <= 0 ||
            offsetAmount.compareTo(payable.getOutstandingAmount()) > 0 ||
            offsetAmount.compareTo(advancePayment.getOutstandingAmount()) > 0) {
            throw new IllegalArgumentException("유효하지 않은 상계 금액입니다.");
        }

        // 매입채무 잔액 감소
        payable.setOutstandingAmount(payable.getOutstandingAmount().subtract(offsetAmount));
        if (payable.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            payable.setStatus(PayableStatus.PAID);
            payable.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            payable.setStatus(PayableStatus.PARTIAL_PAID);
        }
        payableRepository.save(payable);

        // 선급금 잔액 감소
        advancePayment.setOutstandingAmount(advancePayment.getOutstandingAmount().subtract(offsetAmount));
        if (advancePayment.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            advancePayment.setStatus(AdvancePaymentStatus.OFFSET);
            advancePayment.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            advancePayment.setStatus(AdvancePaymentStatus.ACTIVE); // 부분 상계 후에도 활성 상태 유지
        }
        advancePaymentRepository.save(advancePayment);

        // 상계 전표 생성: (차) 매입채무 (대) 선급금
        AccountSubject apAccount = accountSubjectRepository.findById("21100") // 매입채무 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Accounts Payable not found"));
        AccountSubject advancePaymentAccount = accountSubjectRepository.findById("13100") // 선급금 계정 코드 (예시)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Advance Payment not found"));
        Department defaultDepartment = getOrCreateDefaultDepartment();

        JournalEntry offsetEntry = new JournalEntry();
        offsetEntry.setSlipDate(LocalDate.now()); // 상계 처리일
        offsetEntry.setAccountingDate(LocalDate.now());
        offsetEntry.setDescription("선급금과 매입채무 상계: " + payable.getVendor().getBusinessPartnerName() + " - " + offsetAmount);
        offsetEntry.setEntryType("AP_ADVANCE_OFFSET");
        offsetEntry.setLineageSourceType("PAYABLE_OFFSET");
        offsetEntry.setLineageSourceId(payable.getId().toString());
        offsetEntry.setCreatedBy("PAYMENT_MAKER");
        offsetEntry.setAuditUser("PAYMENT_CHECKER");
        offsetEntry.setStatus(JournalEntryStatus.DRAFT);

        // 차변: 매입채무 감소
        JournalDetail debitAp = new JournalDetail();
        debitAp.setDrcrType("DEBIT");
        debitAp.setAccountSubject(apAccount);
        debitAp.setAmount(offsetAmount);
        debitAp.setDepartment(defaultDepartment);
        debitAp.setDetailDescription("매입채무 상계");
        offsetEntry.addDetail(debitAp);

        // 대변: 선급금 감소
        JournalDetail creditAdvancePayment = new JournalDetail();
        creditAdvancePayment.setDrcrType("CREDIT");
        creditAdvancePayment.setAccountSubject(advancePaymentAccount);
        creditAdvancePayment.setAmount(offsetAmount);
        creditAdvancePayment.setDetailDescription("선급금 상계");
        offsetEntry.addDetail(creditAdvancePayment);

        journalService.createJournalEntry(offsetEntry);
        journalService.requestApproval(offsetEntry.getId());
        journalService.approveJournalEntry(offsetEntry.getId());

        return payable;
    }

    private Department getOrCreateDefaultDepartment() {
        return departmentRepository.findByCode("DEFAULT")
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setCode("DEFAULT");
                    department.setName("Default Department");
                    return departmentRepository.save(department);
                });
    }
}
