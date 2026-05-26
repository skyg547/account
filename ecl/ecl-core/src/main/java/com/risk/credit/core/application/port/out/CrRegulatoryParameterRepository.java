package com.risk.credit.core.application.port.out;

import com.risk.credit.core.domain.model.CrRegulatoryParameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrRegulatoryParameterRepository extends JpaRepository<CrRegulatoryParameter, String> {
}
