package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrProductMasterRepository;
import com.ho.account.ecl.core.domain.model.CrProductMaster;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaCrProductMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CrProductMasterPersistenceAdapter implements CrProductMasterRepository {
    private final JpaCrProductMasterRepository jpaRepository;

    @Override public List<CrProductMaster> findAll() { return jpaRepository.findAll(); }

    @Override
    @Cacheable(value = "productMaster", key = "#productCode")
    public Optional<CrProductMaster> findByProductCode(String productCode) {
        return jpaRepository.findByProductCode(productCode);
    }
}
