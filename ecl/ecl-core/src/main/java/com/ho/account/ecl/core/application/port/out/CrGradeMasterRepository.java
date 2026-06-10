package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.CrGradeMaster;
import java.util.List;
import java.util.Optional;

/**
 * [Repository] 신용등급 마스터(Grade Master) 저장소.
 */
public interface CrGradeMasterRepository {
    List<CrGradeMaster> findAll();
    Optional<CrGradeMaster> findByRatingCode(String ratingCode);
}
