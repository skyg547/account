package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsGeneralLedgerEntity;
import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsGeneralLedgerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JpaOdsGeneralLedgerRepository extends JpaRepository<OdsGeneralLedgerEntity, OdsGeneralLedgerId> {
    List<OdsGeneralLedgerEntity> findByBaseDate(LocalDate baseDate);
}
