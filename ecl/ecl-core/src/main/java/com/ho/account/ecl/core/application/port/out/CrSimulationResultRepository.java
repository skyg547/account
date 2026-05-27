package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.result.CrSimulationResult;
import com.ho.account.shared.finance.enums.StressScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [Repository] 스트레스 테스트 시뮬레이션 결과 저장소.
 */
@Repository
public interface CrSimulationResultRepository extends JpaRepository<CrSimulationResult, Long> {
    void deleteByBaseDateAndScenario(LocalDate baseDate, StressScenario scenario);

    List<CrSimulationResult> findByBaseDateAndScenario(LocalDate baseDate, StressScenario scenario);
}
