package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsBalanceHistEntity;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsBalanceHistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaOdsBalanceHistRepository extends JpaRepository<OdsBalanceHistEntity, OdsBalanceHistId> {
    Optional<OdsBalanceHistEntity> findTopByAccountNoOrderByBaseDateDesc(String accountNo);
}
