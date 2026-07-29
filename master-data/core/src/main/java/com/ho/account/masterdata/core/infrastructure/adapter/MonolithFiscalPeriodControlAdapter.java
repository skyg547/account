package com.ho.account.masterdata.core.infrastructure.adapter;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import jakarta.persistence.EntityNotFoundException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MonolithFiscalPeriodControlAdapter implements FiscalPeriodControlPort {

    private final FiscalPeriodPersistencePort fiscalPeriodPersistencePort;

    public MonolithFiscalPeriodControlAdapter(FiscalPeriodPersistencePort fiscalPeriodPersistencePort) {
        this.fiscalPeriodPersistencePort = fiscalPeriodPersistencePort;
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
        fiscalPeriod.changeClosingStatus(parseStatus(closingStatus), auditUser);
        return toRef(fiscalPeriodPersistencePort.save(fiscalPeriod));
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
