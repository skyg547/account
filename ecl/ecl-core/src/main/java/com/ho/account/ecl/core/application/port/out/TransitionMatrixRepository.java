package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.TransitionMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [Repository] 등급 전이 행렬(Transition Matrix) 저장소.
 * 신용 등급간 이동 확률 데이터를 관리합니다.
 */
@Repository
public interface TransitionMatrixRepository extends JpaRepository<TransitionMatrix, Long> {

    List<TransitionMatrix> findByBaseDate(LocalDate baseDate);

    List<TransitionMatrix> findByBaseDateAndFromRating(LocalDate baseDate, String fromRating);
}
