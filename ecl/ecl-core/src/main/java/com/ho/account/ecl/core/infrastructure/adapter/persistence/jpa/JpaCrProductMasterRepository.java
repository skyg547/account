package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.ecl.core.domain.model.CrProductMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaCrProductMasterRepository extends JpaRepository<CrProductMaster, Long> {
    Optional<CrProductMaster> findByProductCode(String productCode);
}
