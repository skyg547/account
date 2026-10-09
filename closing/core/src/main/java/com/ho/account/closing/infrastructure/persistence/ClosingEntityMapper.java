package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.*;

/** Explicit, compile-checked mapping between JPA rows and business aggregates. */
final class ClosingEntityMapper {
    private ClosingEntityMapper() {}

    static ClosingCalendar toDomain(ClosingCalendarEntity entity) {
        if (entity == null) return null;
        ClosingCalendar domain = new ClosingCalendar();
        domain.setId(entity.getId());
        domain.setFiscalYear(entity.getFiscalYear());
        domain.setFiscalPeriod(entity.getFiscalPeriod());
        domain.setStatus(entity.getStatus());
        domain.setCloseInitiatedBy(entity.getCloseInitiatedBy());
        domain.setCloseInitiatedAt(entity.getCloseInitiatedAt());
        domain.setClosedBy(entity.getClosedBy());
        domain.setClosedAt(entity.getClosedAt());
        domain.setReopenedBy(entity.getReopenedBy());
        domain.setReopenedAt(entity.getReopenedAt());
        domain.setCurrentPeriod(entity.isCurrentPeriod());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        domain.setName(entity.getName());
        domain.restoreTransition(entity.getTransitionId(), entity.getTransitionTarget(),
                entity.getTransitionStage(), entity.getTransitionFiscalPeriodId(),
                entity.getTransitionApprovalId(), entity.getTransitionActor(),
                entity.getTransitionPreparedAt(), entity.getTransitionEvidenceSetId());
        return domain;
    }

    static ClosingCalendarEntity toEntity(ClosingCalendar domain) {
        ClosingCalendarEntity entity = new ClosingCalendarEntity();
        entity.setId(domain.getId());
        entity.setFiscalYear(domain.getFiscalYear());
        entity.setFiscalPeriod(domain.getFiscalPeriod());
        entity.setStatus(domain.getStatus());
        entity.setCloseInitiatedBy(domain.getCloseInitiatedBy());
        entity.setCloseInitiatedAt(domain.getCloseInitiatedAt());
        entity.setClosedBy(domain.getClosedBy());
        entity.setClosedAt(domain.getClosedAt());
        entity.setReopenedBy(domain.getReopenedBy());
        entity.setReopenedAt(domain.getReopenedAt());
        entity.setCurrentPeriod(domain.isCurrentPeriod());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        entity.setTransitionId(domain.getTransitionId());
        entity.setTransitionTarget(domain.getTransitionTarget());
        entity.setTransitionStage(domain.getTransitionStage());
        entity.setTransitionFiscalPeriodId(domain.getTransitionFiscalPeriodId());
        entity.setTransitionApprovalId(domain.getTransitionApprovalId());
        entity.setTransitionActor(domain.getTransitionActor());
        entity.setTransitionPreparedAt(domain.getTransitionPreparedAt());
        entity.setTransitionEvidenceSetId(domain.getTransitionEvidenceSetId());
        entity.setName(domain.getName());
        return entity;
    }

    static ClosingTask toDomain(ClosingTaskEntity entity) {
        if (entity == null) return null;
        ClosingTask domain = new ClosingTask();
        domain.setId(entity.getId());
        domain.setClosingCalendar(calendarReference(entity.getClosingCalendar()));
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setCategory(entity.getCategory());
        domain.setDueDate(entity.getDueDate());
        domain.setAssignedTo(entity.getAssignedTo());
        domain.setStatus(entity.getStatus());
        domain.setCompletionConditionJson(entity.getCompletionConditionJson());
        domain.setMandatory(entity.isMandatory());
        domain.setTaskOrder(entity.getTaskOrder());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        domain.setTaskCode(entity.getTaskCode());
        return domain;
    }

    static ClosingTaskEntity toEntity(ClosingTask domain) {
        ClosingTaskEntity entity = new ClosingTaskEntity();
        entity.setId(domain.getId());
        entity.setClosingCalendar(calendarReference(domain.getClosingCalendar()));
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCategory(domain.getCategory());
        entity.setDueDate(domain.getDueDate());
        entity.setAssignedTo(domain.getAssignedTo());
        entity.setStatus(domain.getStatus());
        entity.setCompletionConditionJson(domain.getCompletionConditionJson());
        entity.setMandatory(domain.isMandatory());
        entity.setTaskOrder(domain.getTaskOrder());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        entity.setTaskCode(domain.getTaskCode());
        return entity;
    }

    static ClosingGate toDomain(ClosingGateEntity entity) {
        if (entity == null) return null;
        ClosingGate domain = new ClosingGate();
        domain.setId(entity.getId());
        domain.setClosingCalendar(calendarReference(entity.getClosingCalendar()));
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setStatus(entity.getStatus());
        domain.setCheckConditionJson(entity.getCheckConditionJson());
        domain.setPassedBy(entity.getPassedBy());
        domain.setPassedAt(entity.getPassedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        domain.setGateCode(entity.getGateCode());
        return domain;
    }

    static ClosingGateEntity toEntity(ClosingGate domain) {
        ClosingGateEntity entity = new ClosingGateEntity();
        entity.setId(domain.getId());
        entity.setClosingCalendar(calendarReference(domain.getClosingCalendar()));
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus());
        entity.setCheckConditionJson(domain.getCheckConditionJson());
        entity.setPassedBy(domain.getPassedBy());
        entity.setPassedAt(domain.getPassedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        entity.setGateCode(domain.getGateCode());
        return entity;
    }

    static PeriodLock toDomain(PeriodLockEntity entity) {
        if (entity == null) return null;
        PeriodLock domain = new PeriodLock();
        domain.setId(entity.getId());
        domain.setFiscalPeriodId(entity.getFiscalPeriodId());
        domain.setFiscalYear(entity.getFiscalYear());
        domain.setFiscalPeriod(entity.getFiscalPeriod());
        domain.setLockType(entity.getLockType());
        domain.setLockedBy(entity.getLockedBy());
        domain.setLockedAt(entity.getLockedAt());
        domain.setReason(entity.getReason());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        return domain;
    }

    static PeriodLockEntity toEntity(PeriodLock domain) {
        PeriodLockEntity entity = new PeriodLockEntity();
        entity.setId(domain.getId());
        entity.setFiscalPeriodId(domain.getFiscalPeriodId());
        entity.setFiscalYear(domain.getFiscalYear());
        entity.setFiscalPeriod(domain.getFiscalPeriod());
        entity.setLockType(domain.getLockType());
        entity.setLockedBy(domain.getLockedBy());
        entity.setLockedAt(domain.getLockedAt());
        entity.setReason(domain.getReason());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }

    static ReopenApproval toDomain(ReopenApprovalEntity entity) {
        if (entity == null) return null;
        ReopenApproval domain = new ReopenApproval();
        domain.setId(entity.getId());
        domain.setFiscalPeriodId(entity.getFiscalPeriodId());
        domain.setFiscalYear(entity.getFiscalYear());
        domain.setFiscalPeriod(entity.getFiscalPeriod());
        domain.setRequestedBy(entity.getRequestedBy());
        domain.setRequestedAt(entity.getRequestedAt());
        domain.setReason(entity.getReason());
        domain.setStatus(entity.getStatus());
        domain.setApprovedBy(entity.getApprovedBy());
        domain.setApprovedAt(entity.getApprovedAt());
        domain.setImpactAnalysisReport(entity.getImpactAnalysisReport());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        return domain;
    }

    static ReopenApprovalEntity toEntity(ReopenApproval domain) {
        ReopenApprovalEntity entity = new ReopenApprovalEntity();
        entity.setId(domain.getId());
        entity.setFiscalPeriodId(domain.getFiscalPeriodId());
        entity.setFiscalYear(domain.getFiscalYear());
        entity.setFiscalPeriod(domain.getFiscalPeriod());
        entity.setRequestedBy(domain.getRequestedBy());
        entity.setRequestedAt(domain.getRequestedAt());
        entity.setReason(domain.getReason());
        entity.setStatus(domain.getStatus());
        entity.setApprovedBy(domain.getApprovedBy());
        entity.setApprovedAt(domain.getApprovedAt());
        entity.setImpactAnalysisReport(domain.getImpactAnalysisReport());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }

    static ValuationBatch toDomain(ValuationBatchEntity entity) {
        if (entity == null) return null;
        ValuationBatch domain = new ValuationBatch();
        domain.setId(entity.getId());
        domain.setFiscalPeriodId(entity.getFiscalPeriodId());
        domain.setFiscalYear(entity.getFiscalYear());
        domain.setFiscalPeriod(entity.getFiscalPeriod());
        domain.setValuationType(entity.getValuationType());
        domain.setRunDateTime(entity.getRunDateTime());
        domain.setStatus(entity.getStatus());
        domain.setGeneratedJournalEntryId(entity.getGeneratedJournalEntryId());
        domain.setReportLink(entity.getReportLink());
        domain.setRunBy(entity.getRunBy());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        return domain;
    }

    static ValuationBatchEntity toEntity(ValuationBatch domain) {
        ValuationBatchEntity entity = new ValuationBatchEntity();
        entity.setId(domain.getId());
        entity.setFiscalPeriodId(domain.getFiscalPeriodId());
        entity.setFiscalYear(domain.getFiscalYear());
        entity.setFiscalPeriod(domain.getFiscalPeriod());
        entity.setValuationType(domain.getValuationType());
        entity.setRunDateTime(domain.getRunDateTime());
        entity.setStatus(domain.getStatus());
        entity.setGeneratedJournalEntryId(domain.getGeneratedJournalEntryId());
        entity.setReportLink(domain.getReportLink());
        entity.setRunBy(domain.getRunBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }

    static ProvisionBatch toDomain(ProvisionBatchEntity entity) {
        if (entity == null) return null;
        ProvisionBatch domain = new ProvisionBatch();
        domain.setId(entity.getId());
        domain.setFiscalPeriodId(entity.getFiscalPeriodId());
        domain.setFiscalYear(entity.getFiscalYear());
        domain.setFiscalPeriod(entity.getFiscalPeriod());
        domain.setProvisionType(entity.getProvisionType());
        domain.setRunDateTime(entity.getRunDateTime());
        domain.setStatus(entity.getStatus());
        domain.setGeneratedJournalEntryId(entity.getGeneratedJournalEntryId());
        domain.setReportLink(entity.getReportLink());
        domain.setRunBy(entity.getRunBy());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        return domain;
    }

    static ProvisionBatchEntity toEntity(ProvisionBatch domain) {
        ProvisionBatchEntity entity = new ProvisionBatchEntity();
        entity.setId(domain.getId());
        entity.setFiscalPeriodId(domain.getFiscalPeriodId());
        entity.setFiscalYear(domain.getFiscalYear());
        entity.setFiscalPeriod(domain.getFiscalPeriod());
        entity.setProvisionType(domain.getProvisionType());
        entity.setRunDateTime(domain.getRunDateTime());
        entity.setStatus(domain.getStatus());
        entity.setGeneratedJournalEntryId(domain.getGeneratedJournalEntryId());
        entity.setReportLink(domain.getReportLink());
        entity.setRunBy(domain.getRunBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }

    static ClosingAdjustment toDomain(ClosingAdjustmentEntity entity) {
        if (entity == null) return null;
        ClosingAdjustment domain = new ClosingAdjustment();
        domain.setId(entity.getId());
        domain.setFiscalPeriodId(entity.getFiscalPeriodId());
        domain.setFiscalYear(entity.getFiscalYear());
        domain.setFiscalPeriod(entity.getFiscalPeriod());
        domain.setJournalEntryId(entity.getJournalEntryId());
        domain.setAdjustmentType(entity.getAdjustmentType());
        domain.setDescription(entity.getDescription());
        domain.setApprovedBy(entity.getApprovedBy());
        domain.setApprovedAt(entity.getApprovedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setAuditUser(entity.getAuditUser());
        return domain;
    }

    static ClosingAdjustmentEntity toEntity(ClosingAdjustment domain) {
        ClosingAdjustmentEntity entity = new ClosingAdjustmentEntity();
        entity.setId(domain.getId());
        entity.setFiscalPeriodId(domain.getFiscalPeriodId());
        entity.setFiscalYear(domain.getFiscalYear());
        entity.setFiscalPeriod(domain.getFiscalPeriod());
        entity.setJournalEntryId(domain.getJournalEntryId());
        entity.setAdjustmentType(domain.getAdjustmentType());
        entity.setDescription(domain.getDescription());
        entity.setApprovedBy(domain.getApprovedBy());
        entity.setApprovedAt(domain.getApprovedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAuditUser(domain.getAuditUser());
        return entity;
    }

    static ClosingAuditLog toDomain(ClosingAuditLogEntity entity) {
        if (entity == null) return null;
        ClosingAuditLog domain = new ClosingAuditLog();
        domain.setId(entity.getId());
        domain.setClosingCalendar(calendarReference(entity.getClosingCalendar()));
        domain.setActionType(entity.getActionType());
        domain.setPreviousStatus(entity.getPreviousStatus());
        domain.setCurrentStatus(entity.getCurrentStatus());
        domain.setActionReason(entity.getActionReason());
        domain.setActionUser(entity.getActionUser());
        domain.setActionAt(entity.getActionAt());
        domain.setDetailInfo(entity.getDetailInfo());
        return domain;
    }

    static ClosingAuditLogEntity toEntity(ClosingAuditLog domain) {
        ClosingAuditLogEntity entity = new ClosingAuditLogEntity();
        entity.setId(domain.getId());
        entity.setClosingCalendar(calendarReference(domain.getClosingCalendar()));
        entity.setActionType(domain.getActionType());
        entity.setPreviousStatus(domain.getPreviousStatus());
        entity.setCurrentStatus(domain.getCurrentStatus());
        entity.setActionReason(domain.getActionReason());
        entity.setActionUser(domain.getActionUser());
        entity.setActionAt(domain.getActionAt());
        entity.setDetailInfo(domain.getDetailInfo());
        return entity;
    }

    // Child rows need the aggregate key only; walking a lazy calendar proxy would fail after a port read.
    private static ClosingCalendar calendarReference(ClosingCalendarEntity entity) {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setId(entity.getId());
        return calendar;
    }

    // The adapter replaces this ID-only row with a managed reference before persisting children.
    private static ClosingCalendarEntity calendarReference(ClosingCalendar domain) {
        ClosingCalendarEntity entity = new ClosingCalendarEntity();
        entity.setId(domain.getId());
        return entity;
    }

    static DailyClosingStatus toDomain(DailyClosingStatusEntity entity) {
        if (entity == null) return null;
        return DailyClosingStatus.restore(
                entity.getBusinessDate(),
                entity.getState(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                entity.getPreparedAt(),
                entity.getPreparedBy(),
                entity.getClosingStartedAt(),
                entity.getClosingStartedBy(),
                entity.getClosedAt(),
                entity.getClosedBy(),
                entity.getBodStartedAt(),
                entity.getBodStartedBy(),
                entity.getOpenedAt(),
                entity.getOpenedBy());
    }

    static DailyClosingStatusEntity toEntity(DailyClosingStatus domain) {
        DailyClosingStatusEntity entity = new DailyClosingStatusEntity();
        entity.setBusinessDate(domain.getBusinessDate());
        entity.setState(domain.getState());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setPreparedAt(domain.getPreparedAt());
        entity.setPreparedBy(domain.getPreparedBy());
        entity.setClosingStartedAt(domain.getClosingStartedAt());
        entity.setClosingStartedBy(domain.getClosingStartedBy());
        entity.setClosedAt(domain.getClosedAt());
        entity.setClosedBy(domain.getClosedBy());
        entity.setBodStartedAt(domain.getBodStartedAt());
        entity.setBodStartedBy(domain.getBodStartedBy());
        entity.setOpenedAt(domain.getOpenedAt());
        entity.setOpenedBy(domain.getOpenedBy());
        return entity;
    }
}
