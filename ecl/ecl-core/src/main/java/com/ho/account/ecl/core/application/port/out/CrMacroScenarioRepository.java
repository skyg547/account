package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.CrMacroScenario;

import java.util.List;

public interface CrMacroScenarioRepository {
    List<CrMacroScenario> findAll();
    List<CrMacroScenario> findByApplyYear(Integer applyYear);
}
