package com.ho.account.masterdata.core.infrastructure.adapter;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.application.service.MasterDataActorContext;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import jakarta.persistence.EntityNotFoundException;
import java.util.Locale;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MonolithFiscalPeriodControlAdapter implements FiscalPeriodControlPort {

    private final FiscalPeriodPersistencePort fiscalPeriodPersistencePort;
    private final MasterDataAuditPort auditPort;

    @Autowired
    public MonolithFiscalPeriodControlAdapter(FiscalPeriodPersistencePort fiscalPeriodPersistencePort,
            MasterDataAuditPort auditPort) {
        this.fiscalPeriodPersistencePort = fiscalPeriodPersistencePort;
        this.auditPort = auditPort;
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
        return fiscalPeriodPersistencePort.findById(id).map(this::toRef);
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return fiscalPeriodPersistencePort.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod)
                .map(this::toRef);
    }

    @Override
    @Transactional
    public FiscalPeriodRef updateClosingStatus(Long fiscalPeriodId, String closingStatus, String auditUser) {
        FiscalPeriod fiscalPeriod = fiscalPeriodPersistencePort.findByIdForUpdate(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        FiscalPeriod.ClosingStatus beforeStatus = fiscalPeriod.getClosingStatus();
        fiscalPeriod.changeClosingStatus(parseStatus(closingStatus), auditUser);
        FiscalPeriod saved = fiscalPeriodPersistencePort.save(fiscalPeriod);
        MasterDataActorContext.Actor actor = MasterDataActorContext.current();
        // A signed Closing assertion supplies the separate service/business identities at ingress.
        auditPort.append(new MasterDataAuditEvent("FISCAL_PERIOD_STATUS", Instant.now(),
                MasterDataType.FISCAL_PERIOD, Long.toString(fiscalPeriodId), null, null,
                actor == null ? null : actor.serviceActor(),
                actor == null ? auditUser : actor.businessActor(),
                beforeStatus + " -> " + saved.getClosingStatus(), fiscalPeriodId, saved.getId()));
        return toRef(saved);
    }

    private FiscalPeriod.ClosingStatus parseStatus(String closingStatus) {
        if (closingStatus == null || closingStatus.isBlank()) {
            throw new IllegalArgumentException("Closing status is required.");
        }
        return FiscalPeriod.ClosingStatus.valueOf(closingStatus.trim().toUpperCase(Locale.ROOT));
    }

    private FiscalPeriodRef toRef(FiscalPeriod fiscalPeriod) {
        return new FiscalPeriodRef(
                fiscalPeriod.getId(),
                fiscalPeriod.getFiscalYear(),
                fiscalPeriod.getFiscalPeriod(),
                fiscalPeriod.getStartDate(),
                fiscalPeriod.getEndDate(),
                fiscalPeriod.getClosingStatus() != null ? fiscalPeriod.getClosingStatus().name() : null);
    }
}
