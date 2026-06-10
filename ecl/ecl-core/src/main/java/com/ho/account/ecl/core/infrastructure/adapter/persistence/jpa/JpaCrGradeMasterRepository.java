package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.model.CrGradeMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaCrGradeMasterRepository extends JpaRepository<CrGradeMaster, Long> {
    Optional<CrGradeMaster> findByRatingCode(String ratingCode);
}
