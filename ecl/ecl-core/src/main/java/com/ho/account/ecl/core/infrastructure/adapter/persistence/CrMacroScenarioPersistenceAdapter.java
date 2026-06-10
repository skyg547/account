package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrMacroScenarioRepository;
import com.ho.account.ecl.core.domain.model.CrMacroScenario;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaCrMacroScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CrMacroScenarioPersistenceAdapter implements CrMacroScenarioRepository {
    private final JpaCrMacroScenarioRepository jpaRepository;

    @Override public List<CrMacroScenario> findAll() { return jpaRepository.findAll(); }
    @Override public List<CrMacroScenario> findByApplyYear(Integer applyYear) { return jpaRepository.findByApplyYear(applyYear); }
}
