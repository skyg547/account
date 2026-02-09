package com.ho.account.expenditure.service;

import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.domain.Payment;
import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import com.ho.account.expenditure.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ExpenditureResolutionRepository expenditureRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, ExpenditureResolutionRepository expenditureRepository) {
        this.paymentRepository = paymentRepository;
        this.expenditureRepository = expenditureRepository;
    }

    // 지급 실행
    public Payment executePayment(Long resolutionId, String method) {
        ExpenditureResolution resolution = expenditureRepository.findById(resolutionId)
                .orElseThrow(() -> new IllegalArgumentException("결의서를 찾을 수 없습니다."));

        if (!"APPROVED".equals(resolution.getStatus())) {
            throw new IllegalStateException("승인된 결의서만 지급할 수 있습니다.");
        }

        // 이미 지급되었는지 확인 로직 필요 (여기서는 생략)

        Payment payment = new Payment();
        payment.setExpenditureResolution(resolution);
        payment.setAmount(resolution.getTotalAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod(method);
        payment.setStatus("COMPLETED");

        return paymentRepository.save(payment);
    }
}
