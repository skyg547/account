package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsDqAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JpaOdsDqAuditRepository extends JpaRepository<OdsDqAuditEntity, Long> {
    List<OdsDqAuditEntity> findByBaseDate(LocalDate baseDate);
}
