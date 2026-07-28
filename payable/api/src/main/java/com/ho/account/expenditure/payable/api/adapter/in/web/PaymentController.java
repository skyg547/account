package com.ho.account.expenditure.payable.api.adapter.in.web;

import com.ho.account.expenditure.application.port.in.PaymentUseCase;
import com.ho.account.expenditure.payable.api.dto.AdvancePaymentRequest;
import com.ho.account.expenditure.payable.api.dto.AdvancePaymentResponse;
import com.ho.account.expenditure.payable.api.dto.ExecutePaymentRequest;
import com.ho.account.expenditure.payable.api.dto.OffsetPayableRequest;
import com.ho.account.expenditure.payable.api.dto.PayableResponse;
import com.ho.account.expenditure.payable.api.dto.PaymentResponse;
import com.ho.account.expenditure.payable.api.dto.PaymentRunRequest;
import com.ho.account.expenditure.payable.api.dto.PaymentRunResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [헥사고날 아키텍처 - 인바운드 어댑터 (Inbound Web Adapter)]
 * 지급(Payment) 관련 비즈니스 요청을 처리하는 웹 컨트롤러입니다.
 *
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 시스템의 '지급 창구'입니다.
 * 회계 담당자가 화면에서 "지급 실행" 버튼을 누르면 그 신호를 이 컨트롤러가 받아서,
 * 실제 돈을 보내고 장부를 처리하는 core 서비스(`PaymentService`)에게 일을 시킵니다.
 * 외부의 HTTP 요청을 애플리케이션 내부에서 이해할 수 있는 command로 바꿔주는 변환기 역할을 합니다.
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
    public ResponseEntity<PaymentRunResponse> initiatePaymentRun(@Valid @RequestBody PaymentRunRequest request) {
        return ResponseEntity.ok(PaymentRunResponse.from(paymentUseCase.initiatePaymentRun(request.toCommand())));
    }

    /**
     * 개별 지급을 실행합니다.
     */
    @PostMapping("/execute")
    public ResponseEntity<PaymentResponse> executePayment(@Valid @RequestBody ExecutePaymentRequest request) {
        return ResponseEntity.ok(PaymentResponse.from(paymentUseCase.executePayment(request.toCommand())));
    }

    /**
     * 선급금을 기록합니다.
     */
    @PostMapping("/advance")
    public ResponseEntity<AdvancePaymentResponse> recordAdvancePayment(@Valid @RequestBody AdvancePaymentRequest request) {
        return ResponseEntity.ok(AdvancePaymentResponse.from(paymentUseCase.recordAdvancePayment(request.toCommand())));
    }

    /**
     * 채무와 선급금을 상계합니다.
     */
    @PostMapping("/offset-payable")
    public ResponseEntity<PayableResponse> offsetPayableWithAdvancePayment(@Valid @RequestBody OffsetPayableRequest request) {
        return ResponseEntity.ok(PayableResponse.from(paymentUseCase.offsetPayableWithAdvancePayment(request.toCommand())));
    }
}