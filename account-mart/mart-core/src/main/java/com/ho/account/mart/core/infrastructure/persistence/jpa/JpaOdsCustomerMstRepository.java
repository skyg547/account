package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsCustomerMstEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaOdsCustomerMstRepository extends JpaRepository<OdsCustomerMstEntity, String> {
    Optional<OdsCustomerMstEntity> findByCustomerCode(String customerCode);
}
