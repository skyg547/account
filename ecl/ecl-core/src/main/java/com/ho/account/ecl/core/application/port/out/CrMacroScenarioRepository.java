package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.CrMacroScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrMacroScenarioRepository extends JpaRepository<CrMacroScenario, Long> {
    List<CrMacroScenario> findByApplyYear(Integer applyYear);
}
