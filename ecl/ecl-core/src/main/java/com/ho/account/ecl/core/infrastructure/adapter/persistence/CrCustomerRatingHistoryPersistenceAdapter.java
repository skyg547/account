package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrCustomerRatingHistoryRepository;
import com.ho.account.ecl.core.domain.exposure.CrCustomerRatingHistory;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaCrCustomerRatingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CrCustomerRatingHistoryPersistenceAdapter implements CrCustomerRatingHistoryRepository {
    private final JpaCrCustomerRatingHistoryRepository jpaRepository;

    @Override
    public List<CrCustomerRatingHistory> findByCustomer_IdOrderByBaseDateDesc(Long customerId) {
        return jpaRepository.findByCustomer_IdOrderByBaseDateDesc(customerId);
    }
}
