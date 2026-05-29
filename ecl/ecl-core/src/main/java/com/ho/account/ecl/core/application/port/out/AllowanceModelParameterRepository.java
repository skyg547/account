package com.ho.account.ecl.core.application.port.out;

import com.ho.account.ecl.core.domain.model.AllowanceModelParameter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowanceModelParameterRepository extends JpaRepository<AllowanceModelParameter, String> {
}
