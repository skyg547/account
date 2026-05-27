package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.ods.OdsBehavioralHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JpaOdsBehavioralHistoryRepository extends JpaRepository<OdsBehavioralHistoryEntity, Long> {
    List<OdsBehavioralHistoryEntity> findByAccountNoAndBaseDateBetween(String accountNo, LocalDate startDate, LocalDate endDate);
}