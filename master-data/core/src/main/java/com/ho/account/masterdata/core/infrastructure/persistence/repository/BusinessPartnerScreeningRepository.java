package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.BusinessPartnerScreeningJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessPartnerScreeningRepository extends JpaRepository<BusinessPartnerScreeningJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT screening FROM BusinessPartnerScreeningJpaEntity screening WHERE screening.businessPartnerCode = :code")
    Optional<BusinessPartnerScreeningJpaEntity> findByCodeForUpdate(@Param("code") String code);
}
