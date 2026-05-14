package com.ho.account.masterdata.core.infrastructure.adapter;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MonolithFiscalPeriodControlAdapter implements FiscalPeriodControlPort {

    private final FiscalPeriodRepository fiscalPeriodRepository;

    public MonolithFiscalPeriodControlAdapter(FiscalPeriodRepository fiscalPeriodRepository) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
        return fiscalPeriodRepository.findById(id).map(this::toRef);
    }

    @Override
    public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
        return fiscalPeriodRepository.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod)
                .map(this::toRef);
    }

    @Override
    public FiscalPeriodRef updateClosingStatus(Long fiscalPeriodId, String closingStatus, String auditUser) {
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        fiscalPeriod.setClosingStatus(FiscalPeriod.ClosingStatus.valueOf(closingStatus));
        fiscalPeriod.setAuditUser(auditUser);
        return toRef(fiscalPeriodRepository.save(fiscalPeriod));
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
