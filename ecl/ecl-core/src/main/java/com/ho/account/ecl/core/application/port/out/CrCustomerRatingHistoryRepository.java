package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.exposure.CrCustomerRatingHistory;

import java.util.List;

/**
 * 고객 신용등급 이력 저장소
 */
public interface CrCustomerRatingHistoryRepository {
    List<CrCustomerRatingHistory> findByCustomer_IdOrderByBaseDateDesc(Long customerId);
}
