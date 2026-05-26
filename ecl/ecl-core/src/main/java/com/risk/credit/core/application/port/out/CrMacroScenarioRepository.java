package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.model.CrMacroScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrMacroScenarioRepository extends JpaRepository<CrMacroScenario, Long> {
    List<CrMacroScenario> findByApplyYear(Integer applyYear);
}
