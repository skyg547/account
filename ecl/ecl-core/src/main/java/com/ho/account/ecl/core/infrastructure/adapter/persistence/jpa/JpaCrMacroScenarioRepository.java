package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.model.CrMacroScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaCrMacroScenarioRepository extends JpaRepository<CrMacroScenario, Long> {
    List<CrMacroScenario> findByApplyYear(Integer applyYear);
}
