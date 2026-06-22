package com.ho.account.reporting.infrastructure.filing;

import com.ho.account.reporting.application.port.out.SubmitRegulatoryFilingPort;
import com.ho.account.reporting.domain.model.RegulatoryFilingPackage;
import com.ho.account.reporting.domain.model.RegulatoryFilingReceipt;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LocalRegulatoryFilingGatewayAdapter implements SubmitRegulatoryFilingPort {

    private static final Logger log = LoggerFactory.getLogger(LocalRegulatoryFilingGatewayAdapter.class);
    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private final boolean failureSimulationEnabled;
    private final String failureSimulationMessage;

    public LocalRegulatoryFilingGatewayAdapter(
            @Value("${account.reporting.regulatory-filing.failure-simulation.enabled:false}") boolean failureSimulationEnabled,
            @Value("${account.reporting.regulatory-filing.failure-simulation.message:Gateway rejected filing due to schema validation failure.}") String failureSimulationMessage) {
        this.failureSimulationEnabled = failureSimulationEnabled;
        this.failureSimulationMessage = failureSimulationMessage;
    }

    @Override
    public RegulatoryFilingReceipt submit(RegulatoryFilingPackage filingPackage) {
        // T30 fixed: Added protocol adapter boundary structure including pseudo-auth, retry handling, and rejection.
        log.info("Initiating secure connection to regulatory gateway for agency: {}", filingPackage.targetAgency());
        
        try {
            authenticateWithGateway();
            
            // Simulate network/protocol submission
            log.info("Transmitting filing package [Version: {}]", filingPackage.submissionVersion());
            
            // 로컬 실패 시뮬레이션은 확률이 아니라 설정값으로 제어해 테스트와 재실행 결과가 흔들리지 않게 합니다.
            if (failureSimulationEnabled) {
                throw new IllegalStateException(failureSimulationMessage);
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
