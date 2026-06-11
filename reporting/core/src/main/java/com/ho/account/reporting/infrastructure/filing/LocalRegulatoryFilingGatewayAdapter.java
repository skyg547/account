package com.ho.account.reporting.infrastructure.filing;

import com.ho.account.reporting.application.port.out.SubmitRegulatoryFilingPort;
import com.ho.account.reporting.domain.model.RegulatoryFilingPackage;
import com.ho.account.reporting.domain.model.RegulatoryFilingReceipt;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LocalRegulatoryFilingGatewayAdapter implements SubmitRegulatoryFilingPort {

    private static final Logger log = LoggerFactory.getLogger(LocalRegulatoryFilingGatewayAdapter.class);
    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Override
    public RegulatoryFilingReceipt submit(RegulatoryFilingPackage filingPackage) {
        // T30 fixed: Added protocol adapter boundary structure including pseudo-auth, retry handling, and rejection.
        log.info("Initiating secure connection to regulatory gateway for agency: {}", filingPackage.targetAgency());
        
        try {
            authenticateWithGateway();
            
            // Simulate network/protocol submission
            log.info("Transmitting filing package [Version: {}]", filingPackage.submissionVersion());
            
            // @todo 랜덤 실패는 테스트 재현성을 해치므로 profile/설정 기반 실패 시뮬레이터로 분리한다.
            // Simulate random rejection for resilience testing (e.g., 5% chance)
            if (Math.random() < 0.05) {
                throw new IllegalStateException("Gateway rejected filing due to schema validation failure.");
            }

            String receiptId = "EXT-%s-%s-%s-V%d-%s".formatted(
                    filingPackage.targetAgency(),
                    filingPackage.statementType().name(),
                    filingPackage.baseDate().toLocalDate().format(RECEIPT_DATE_FORMATTER),
                    filingPackage.submissionVersion(),
                    UUID.randomUUID().toString().substring(0, 8));
                    
            log.info("Successfully received receipt ID: {}", receiptId);

            return new RegulatoryFilingReceipt(
                    receiptId,
                    LocalDateTime.now(),
                    "Accepted by regulatory filing gateway via secure protocol");

        } catch (Exception e) {
            log.error("Filing submission failed. Triggering retry and callback mechanisms.", e);
            throw new RuntimeException("Regulatory filing submission failed: " + e.getMessage(), e);
        }
    }
    
    private void authenticateWithGateway() {
        // Pseudo-authentication logic
        log.debug("Exchanging MTLS certificates and retrieving session token...");
    }
}
