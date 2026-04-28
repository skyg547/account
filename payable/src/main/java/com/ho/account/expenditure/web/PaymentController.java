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
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort; // For mapping request DTO to domain object

    public PaymentController(PaymentService paymentService, PaymentRepository paymentRepository, BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    /**
     * ?덈줈??吏湲??ㅽ뻾???쒖옉?섍퀬, 留뚭린?쇱씠 ?꾨옒??留ㅼ엯梨꾨Т瑜???곸쑝濡?吏湲됱쓣 ?앹꽦?⑸땲??
     * @param request 吏湲??ㅽ뻾 ?붿껌 DTO
     * @return ?앹꽦??吏湲??ㅽ뻾 ?뺣낫
     */
    @PostMapping("/run")
    public ResponseEntity<PaymentRun> initiatePaymentRun(@Valid @RequestBody PaymentRunRequest request) {
        PaymentRun paymentRun = paymentService.initiatePaymentRun(request.getRunDate(), request.getDescription(), request.getCreatedBy());
        return new ResponseEntity<>(paymentRun, HttpStatus.CREATED);
    }

    /**
     * ?뱀젙 吏湲??ㅽ뻾 ID???ы븿??吏湲?紐⑸줉??議고쉶?⑸땲??
     * @param paymentRunId 吏湲??ㅽ뻾 ID
     * @return 吏湲?紐⑸줉
     */
    @GetMapping("/run/{paymentRunId}")
    public ResponseEntity<List<Payment>> getPaymentsInRun(@PathVariable Long paymentRunId) {
        List<Payment> payments = paymentRepository.findByPaymentRunId(paymentRunId);
        return ResponseEntity.ok(payments);
    }


    /**
     * 吏湲됱쓣 ?ㅽ뻾?섍퀬 愿???꾪몴瑜??앹꽦?⑸땲?? (?④굔 吏湲??먮뒗 吏湲??ㅽ뻾 ??吏湲?泥섎━)
     * @param request 吏湲??ㅽ뻾 ?붿껌 DTO
     * @return ?꾨즺??吏湲??뺣낫
     */
    @PostMapping("/execute")
    public ResponseEntity<Payment> executePayment(@Valid @RequestBody ExecutePaymentRequest request) {
        Payment payment = paymentService.executePayment(request.getPaymentId(), request.getBankAccount());
        return ResponseEntity.ok(payment);
    }

    /**
     * ?좉툒湲덉쓣 湲곕줉?섍퀬 ?꾪몴瑜??앹꽦?⑸땲??
     * @param request ?좉툒湲??깅줉 ?붿껌 DTO
     * @return ?앹꽦???좉툒湲??뺣낫
     */
    @PostMapping("/advance")
    public ResponseEntity<AdvancePayment> recordAdvancePayment(@Valid @RequestBody AdvancePaymentRequest request) {
        BusinessPartner vendor = businessPartnerPersistencePort.findByBusinessPartnerCode(request.getVendorCode())
                .orElseThrow(() -> new IllegalArgumentException("怨듦툒?낆껜 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎: " + request.getVendorCode()));

        AdvancePayment advancePayment = new AdvancePayment();
        advancePayment.setVendor(vendor);
        advancePayment.setPaymentDate(request.getPaymentDate());
        advancePayment.setAmount(request.getAmount());
        advancePayment.setDescription(request.getDescription());

        AdvancePayment createdAdvancePayment = paymentService.recordAdvancePayment(advancePayment);
        return new ResponseEntity<>(createdAdvancePayment, HttpStatus.CREATED);
    }

    /**
     * 留ㅼ엯梨꾨Т瑜??좉툒湲덇낵 ?곴퀎 泥섎━?⑸땲??
     * @param request ?곴퀎 ?붿껌 DTO
     * @return ?낅뜲?댄듃??留ㅼ엯梨꾨Т ?뺣낫
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
