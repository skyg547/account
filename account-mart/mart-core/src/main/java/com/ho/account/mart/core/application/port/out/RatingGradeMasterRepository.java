package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.external.kap.RatingGradeMaster;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 외부 신용등급 마스터 데이터 접근 인터페이스
 */
public interface RatingGradeMasterRepository {
    Optional<RatingGradeMaster> findByGradeCodeIgnoreCaseAndIsActiveTrue(String gradeCode);
    List<RatingGradeMaster> findAll();
    RatingGradeMaster save(RatingGradeMaster gradeMaster);
}
