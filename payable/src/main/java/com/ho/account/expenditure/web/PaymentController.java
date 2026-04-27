package com.ho.account.expenditure.web;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
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
     * ?ˆë¡œ??ì§€ê¸??¤í–‰???œì‘?˜ê³ , ë§Œê¸°?¼ì´ ?„ë˜??ë§¤ì…ì±„ë¬´ë¥??€?ìœ¼ë¡?ì§€ê¸‰ì„ ?ì„±?©ë‹ˆ??
     * @param request ì§€ê¸??¤í–‰ ?”ì²­ DTO
     * @return ?ì„±??ì§€ê¸??¤í–‰ ?•ë³´
     */
    @PostMapping("/run")
    public ResponseEntity<PaymentRun> initiatePaymentRun(@Valid @RequestBody PaymentRunRequest request) {
        PaymentRun paymentRun = paymentService.initiatePaymentRun(request.getRunDate(), request.getDescription(), request.getCreatedBy());
        return new ResponseEntity<>(paymentRun, HttpStatus.CREATED);
    }

    /**
     * ?¹ì • ì§€ê¸??¤í–‰ ID???¬í•¨??ì§€ê¸?ëª©ë¡??ì¡°íšŒ?©ë‹ˆ??
     * @param paymentRunId ì§€ê¸??¤í–‰ ID
     * @return ì§€ê¸?ëª©ë¡
     */
    @GetMapping("/run/{paymentRunId}")
    public ResponseEntity<List<Payment>> getPaymentsInRun(@PathVariable Long paymentRunId) {
        List<Payment> payments = paymentRepository.findByPaymentRunId(paymentRunId);
        return ResponseEntity.ok(payments);
    }


    /**
     * ì§€ê¸‰ì„ ?¤í–‰?˜ê³  ê´€???„í‘œë¥??ì„±?©ë‹ˆ?? (?¨ê±´ ì§€ê¸??ëŠ” ì§€ê¸??¤í–‰ ??ì§€ê¸?ì²˜ë¦¬)
     * @param request ì§€ê¸??¤í–‰ ?”ì²­ DTO
     * @return ?„ë£Œ??ì§€ê¸??•ë³´
     */
    @PostMapping("/execute")
    public ResponseEntity<Payment> executePayment(@Valid @RequestBody ExecutePaymentRequest request) {
        Payment payment = paymentService.executePayment(request.getPaymentId(), request.getBankAccount());
        return ResponseEntity.ok(payment);
    }

    /**
     * ? ê¸‰ê¸ˆì„ ê¸°ë¡?˜ê³  ?„í‘œë¥??ì„±?©ë‹ˆ??
     * @param request ? ê¸‰ê¸??±ë¡ ?”ì²­ DTO
     * @return ?ì„±??? ê¸‰ê¸??•ë³´
     */
    @PostMapping("/advance")
    public ResponseEntity<AdvancePayment> recordAdvancePayment(@Valid @RequestBody AdvancePaymentRequest request) {
        BusinessPartner vendor = businessPartnerRepository.findByBusinessPartnerCode(request.getVendorCode())
                .orElseThrow(() -> new IllegalArgumentException("ê³µê¸‰?…ì²´ ?•ë³´ë¥?ì°¾ì„ ???†ìŠµ?ˆë‹¤: " + request.getVendorCode()));

        AdvancePayment advancePayment = new AdvancePayment();
        advancePayment.setVendor(vendor);
        advancePayment.setPaymentDate(request.getPaymentDate());
        advancePayment.setAmount(request.getAmount());
        advancePayment.setDescription(request.getDescription());

        AdvancePayment createdAdvancePayment = paymentService.recordAdvancePayment(advancePayment);
        return new ResponseEntity<>(createdAdvancePayment, HttpStatus.CREATED);
    }

    /**
     * ë§¤ì…ì±„ë¬´ë¥?? ê¸‰ê¸ˆê³¼ ?ê³„ ì²˜ë¦¬?©ë‹ˆ??
     * @param request ?ê³„ ?”ì²­ DTO
     * @return ?…ë°?´íŠ¸??ë§¤ì…ì±„ë¬´ ?•ë³´
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
