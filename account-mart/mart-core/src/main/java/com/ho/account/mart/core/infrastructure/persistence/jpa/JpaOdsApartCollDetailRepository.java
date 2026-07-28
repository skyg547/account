package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsApartCollDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [Infrastructure] 아파트 담보 상세 JPA 리포지토리.
 */
@Repository
public interface JpaOdsApartCollDetailRepository extends JpaRepository<OdsApartCollDetailEntity, String> {
}
