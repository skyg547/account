package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsReconcileHistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JpaOdsReconcileHistRepository extends JpaRepository<OdsReconcileHistEntity, Long> {
    List<OdsReconcileHistEntity> findByBaseDate(LocalDate baseDate);
}
