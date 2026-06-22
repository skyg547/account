package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsProductMstEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaOdsProductMstRepository extends JpaRepository<OdsProductMstEntity, String> {
}
