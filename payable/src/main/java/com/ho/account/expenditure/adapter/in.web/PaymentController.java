package com.ho.account.expenditure.adapter.in.web;

import com.ho.account.expenditure.application.port.in.PaymentUseCase;
import com.ho.account.expenditure.domain.AdvancePayment;
import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.domain.PaymentRun;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    /**
     * 지급 런을 생성합니다.
     */
    @PostMapping("/run")
    public ResponseEntity<PaymentRun> initiatePaymentRun(@RequestBody Map<String, Object> request) {
        LocalDate runDate = LocalDate.parse((String) request.get("runDate"));
        String description = (String) request.get("description");
        String createdBy = (String) request.get("createdBy");
        return ResponseEntity.ok(paymentUseCase.initiatePaymentRun(runDate, description, createdBy));
    }

    /**
     * 개별 지급을 실행합니다.
     */
    @PostMapping("/execute")
    public ResponseEntity<Payment> executePayment(@RequestBody Map<String, Object> request) {
        Long paymentId = Long.valueOf(request.get("paymentId").toString());
        String bankAccount = (String) request.get("bankAccount");
        return ResponseEntity.ok(paymentUseCase.executePayment(paymentId, bankAccount));
    }

    /**
     * 선급금을 기록합니다.
     */
    @PostMapping("/advance")
    public ResponseEntity<AdvancePayment> recordAdvancePayment(@RequestBody AdvancePayment advancePayment) {
        return ResponseEntity.ok(paymentUseCase.recordAdvancePayment(advancePayment));
    }

    /**
     * 채무와 선급금을 상계합니다.
     */
    @PostMapping("/offset-payable")
    public ResponseEntity<Payable> offsetPayableWithAdvancePayment(@RequestBody Map<String, Object> request) {
        Long payableId = Long.valueOf(request.get("payableId").toString());
        Long advancePaymentId = Long.valueOf(request.get("advancePaymentId").toString());
        BigDecimal offsetAmount = new BigDecimal(request.get("offsetAmount").toString());
        return ResponseEntity.ok(paymentUseCase.offsetPayableWithAdvancePayment(payableId, advancePaymentId, offsetAmount));
    }
}
