package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.Payable;
import com.ho.account.expenditure.domain.PayableStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayablePersistencePort {
    Payable save(Payable payable);
    Optional<Payable> findById(Long id);
    /**
     * 상계할 채무 한 건을 호출자의 트랜잭션이 끝날 때까지 잠가 최신 잔액을 읽습니다.
     * 같은 채무 행을 갱신하는 지급 claim과 상계를 직렬화합니다.
     */
    Optional<Payable> findByIdForUpdate(Long id);
    /**
     * 적격 상태와 양수 잔액인 채무 한 건만 DB에서 IN_PAYMENT로 조건부 전이합니다.
     * 호출자의 트랜잭션 안에서 실행하며, 성공하면 1, 현재 DB 상태가 조건에 맞지 않으면 0을 반환합니다.
     */
    int claimForPayment(Long payableId);
    List<Payable> findByDueDateBeforeAndStatusNot(LocalDate date, PayableStatus status);
    List<Payable> findByVendorCodeAndOutstandingAmountGreaterThan(String vendorCode, BigDecimal amount);
}
