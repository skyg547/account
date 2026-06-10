package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.exposure.CrCustomerRatingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCrCustomerRatingHistoryRepository extends JpaRepository<CrCustomerRatingHistory, Long> {
    List<CrCustomerRatingHistory> findByCustomer_IdOrderByBaseDateDesc(Long customerId);
}
