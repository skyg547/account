package com.ho.account.reporting.application.service;

import com.ho.account.reporting.application.port.in.SubmitRegulatoryFilingUseCase;
import com.ho.account.reporting.application.port.out.LoadDisclosureNoteMartPort;
import com.ho.account.reporting.application.port.out.LoadRegulatoryFilingPort;
import com.ho.account.reporting.application.port.out.LoadRegulatoryReportMappingPort;
import com.ho.account.reporting.application.port.out.LoadRegulatoryReportSubmissionPort;
import com.ho.account.reporting.application.port.out.StoreRegulatoryFilingPort;
import com.ho.account.reporting.application.port.out.SubmitRegulatoryFilingPort;
import com.ho.account.reporting.domain.model.DisclosureNoteMart;
import com.ho.account.reporting.domain.model.RegulatoryFiling;
import com.ho.account.reporting.domain.model.RegulatoryFilingPackage;
import com.ho.account.reporting.domain.model.RegulatoryFilingReceipt;
import com.ho.account.reporting.domain.model.RegulatoryReportSubmission;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegulatoryFilingService implements SubmitRegulatoryFilingUseCase {

    private final LoadRegulatoryReportSubmissionPort loadRegulatoryReportSubmissionPort;
    private final LoadDisclosureNoteMartPort loadDisclosureNoteMartPort;
    private final LoadRegulatoryReportMappingPort loadRegulatoryReportMappingPort;
    private final SubmitRegulatoryFilingPort submitRegulatoryFilingPort;
    private final StoreRegulatoryFilingPort storeRegulatoryFilingPort;
    private final LoadRegulatoryFilingPort loadRegulatoryFilingPort;

    @Override
    public RegulatoryFiling submit(SubmitCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String targetAgency = hasText(command.targetAgency()) ? command.targetAgency().trim() : "FSS";
        RegulatoryReportSubmission submission = loadRegulatoryReportSubmissionPort
                .findLatestReady(command.type(), command.baseDate())
                .orElseThrow(() -> new IllegalStateException(
                        "Ready regulatory report submission is required before filing."));
        DisclosureNoteMart mart = loadDisclosureNoteMartPort.find(command.type(), command.baseDate())
                .orElseThrow(() -> new IllegalStateException(
                        "Disclosure note mart is required before regulatory filing."));
        RegulatoryFilingPackage filingPackage = RegulatoryFiling.packageFor(
                submission,
                mart,
                loadRegulatoryReportMappingPort.loadMappings(command.type(), command.baseDate()),
                targetAgency);
        RegulatoryFilingReceipt receipt = submitRegulatoryFilingPort.submit(filingPackage);
        RegulatoryFiling filing = RegulatoryFiling.accepted(
                filingPackage,
                command.requesterId(),
                LocalDateTime.now(),
                receipt);
        storeRegulatoryFilingPort.save(filing);
        return filing;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegulatoryFiling> findLatest(FindLatestQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return loadRegulatoryFilingPort.findLatest(query.type(), query.baseDate());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
