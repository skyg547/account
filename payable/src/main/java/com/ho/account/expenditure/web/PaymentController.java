package com.ho.account.expenditure.web;

import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.dto.AdvancePaymentRequest;
import com.ho.account.expenditure.dto.ExecutePaymentRequest;
import com.ho.account.expenditure.dto.OffsetPayableRequest;
import com.ho.account.expenditure.dto.PaymentRunRequest;
import com.ho.account.expenditure.repository.PaymentRepository; // For fetching payment details
import com.ho.account.expenditure.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository; // For fetching payment details
    private final BusinessPartnerRepository businessPartnerRepository; // For mapping request DTO to domain object

    public PaymentController(PaymentService paymentService, PaymentRepository paymentRepository, BusinessPartnerRepository businessPartnerRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    /**
     * 새로운 지급 실행을 시작하고, 만기일이 도래한 매입채무를 대상으로 지급을 생성합니다.
     * @param request 지급 실행 요청 DTO
     * @return 생성된 지급 실행 정보
     */
    @PostMapping("/run")
    public ResponseEntity<PaymentRun> initiatePaymentRun(@Valid @RequestBody PaymentRunRequest request) {
        PaymentRun paymentRun = paymentService.initiatePaymentRun(request.getRunDate(), request.getDescription(), request.getCreatedBy());
        return new ResponseEntity<>(paymentRun, HttpStatus.CREATED);
    }

    /**
     * 특정 지급 실행 ID에 포함된 지급 목록을 조회합니다.
     * @param paymentRunId 지급 실행 ID
     * @return 지급 목록
     */
    @GetMapping("/run/{paymentRunId}")
    public ResponseEntity<List<Payment>> getPaymentsInRun(@PathVariable Long paymentRunId) {
        List<Payment> payments = paymentRepository.findByPaymentRunId(paymentRunId);
        return ResponseEntity.ok(payments);
    }


    /**
     * 지급을 실행하고 관련 전표를 생성합니다. (단건 지급 또는 지급 실행 내 지급 처리)
     * @param request 지급 실행 요청 DTO
     * @return 완료된 지급 정보
     */
    @PostMapping("/execute")
    public ResponseEntity<Payment> executePayment(@Valid @RequestBody ExecutePaymentRequest request) {
        Payment payment = paymentService.executePayment(request.getPaymentId(), request.getBankAccount());
        return ResponseEntity.ok(payment);
    }

    /**
     * 선급금을 기록하고 전표를 생성합니다.
     * @param request 선급금 등록 요청 DTO
     * @return 생성된 선급금 정보
     */
    @PostMapping("/advance")
    public ResponseEntity<AdvancePayment> recordAdvancePayment(@Valid @RequestBody AdvancePaymentRequest request) {
        BusinessPartner vendor = businessPartnerRepository.findByBusinessPartnerCode(request.getVendorCode())
                .orElseThrow(() -> new IllegalArgumentException("공급업체 정보를 찾을 수 없습니다: " + request.getVendorCode()));

        AdvancePayment advancePayment = new AdvancePayment();
        advancePayment.setVendor(vendor);
        advancePayment.setPaymentDate(request.getPaymentDate());
        advancePayment.setAmount(request.getAmount());
        advancePayment.setDescription(request.getDescription());

        AdvancePayment createdAdvancePayment = paymentService.recordAdvancePayment(advancePayment);
        return new ResponseEntity<>(createdAdvancePayment, HttpStatus.CREATED);
    }

    /**
     * 매입채무를 선급금과 상계 처리합니다.
     * @param request 상계 요청 DTO
     * @return 업데이트된 매입채무 정보
     */
    @PostMapping("/offset-payable")
    public ResponseEntity<Payable> offsetPayableWithAdvancePayment(@Valid @RequestBody OffsetPayableRequest request) {
        Payable updatedPayable = paymentService.offsetPayableWithAdvancePayment(
                request.getPayableId(),
                request.getAdvancePaymentId(),
                request.getOffsetAmount()
        );
        return ResponseEntity.ok(updatedPayable);
    }
}
