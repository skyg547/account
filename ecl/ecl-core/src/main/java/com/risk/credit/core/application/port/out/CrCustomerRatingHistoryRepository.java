package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.exposure.CrCustomerRatingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 고객 신용등급 이력 저장소
 */
@Repository
public interface CrCustomerRatingHistoryRepository extends JpaRepository<CrCustomerRatingHistory, Long> {
    List<CrCustomerRatingHistory> findByCustomer_IdOrderByBaseDateDesc(Long customerId);
}
