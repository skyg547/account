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

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 지급(Payment) 관련 비즈니스 요청을 처리하는 웹 컨트롤러입니다.
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템의 '지급 창구'입니다.
 * 회계 담당자가 화면에서 "지급 실행" 버튼을 누르면 그 신호를 이 컨트롤러가 받아서, 
 * 실제 돈을 보내고 장부를 처리하는 내부 서비스(`PaymentService`)에게 일을 시킵니다.
 * 외부의 HTTP 요청을 애플리케이션 내부에서 이해할 수 있는 명령으로 바꿔주는 변환기 역할을 합니다.
 */
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
        // @todo raw Map 대신 PaymentRunRequest DTO와 Bean Validation을 사용해 API 계약을 명확히 한다.
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
        // @todo ExecutePaymentRequest DTO로 전환해 paymentId/bankAccount 필수값 검증을 컨트롤러 경계에서 수행한다.
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
        // @todo OffsetPayableRequest DTO를 사용해 상계 금액 형식과 필수 ID를 명시적으로 검증한다.
        Long payableId = Long.valueOf(request.get("payableId").toString());
        Long advancePaymentId = Long.valueOf(request.get("advancePaymentId").toString());
        BigDecimal offsetAmount = new BigDecimal(request.get("offsetAmount").toString());
        return ResponseEntity.ok(paymentUseCase.offsetPayableWithAdvancePayment(payableId, advancePaymentId, offsetAmount));
    }
}
