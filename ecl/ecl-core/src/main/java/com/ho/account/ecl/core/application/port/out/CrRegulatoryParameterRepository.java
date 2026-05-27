package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.CrRegulatoryParameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrRegulatoryParameterRepository extends JpaRepository<CrRegulatoryParameter, String> {
}
