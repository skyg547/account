package com.ho.account.reporting.infrastructure.filing;

import com.ho.account.reporting.application.port.out.SubmitRegulatoryFilingPort;
import com.ho.account.reporting.domain.model.RegulatoryFilingPackage;
import com.ho.account.reporting.domain.model.RegulatoryFilingReceipt;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class LocalRegulatoryFilingGatewayAdapter implements SubmitRegulatoryFilingPort {

    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Override
    public RegulatoryFilingReceipt submit(RegulatoryFilingPackage filingPackage) {
        String receiptId = "LOCAL-%s-%s-%s-V%d".formatted(
                filingPackage.targetAgency(),
                filingPackage.statementType().name(),
                filingPackage.baseDate().toLocalDate().format(RECEIPT_DATE_FORMATTER),
                filingPackage.submissionVersion());
        return new RegulatoryFilingReceipt(
                receiptId,
                LocalDateTime.now(),
                "Accepted by local regulatory filing gateway");
    }
}
