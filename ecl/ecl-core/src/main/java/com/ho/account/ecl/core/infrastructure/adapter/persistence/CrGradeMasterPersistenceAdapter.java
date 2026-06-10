package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrGradeMasterRepository;
import com.ho.account.ecl.core.domain.model.CrGradeMaster;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaCrGradeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CrGradeMasterPersistenceAdapter implements CrGradeMasterRepository {
    private final JpaCrGradeMasterRepository jpaRepository;

    @Override public List<CrGradeMaster> findAll() { return jpaRepository.findAll(); }

    @Override
    @Cacheable(value = "gradeMaster", key = "#ratingCode")
    public Optional<CrGradeMaster> findByRatingCode(String ratingCode) {
        return jpaRepository.findByRatingCode(ratingCode);
    }
}
