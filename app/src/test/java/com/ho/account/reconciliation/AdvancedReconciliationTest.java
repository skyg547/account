package com.ho.account.reconciliation;

import com.ho.account.reconciliation.domain.*;
import com.ho.account.reconciliation.repository.ReconUnitDefinitionRepository;
import com.ho.account.reconciliation.service.ReconManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdvancedReconciliationTest {

    @Autowired
    private ReconManagerService reconManagerService;

    @Autowired
    private ReconUnitDefinitionRepository unitRepository;

    @Test
    @DisplayName("4단계 심층 대사 프로세스 검증")
    void testDeepReconciliation() {
        // 1. 대사 단위 정의
        ReconUnitDefinition unit = new ReconUnitDefinition();
        unit.setUnitId("UNIT-LOAN-001");
        unit.setUnitName("대출 원금 대사");
        unit.setReconType(ReconciliationType.SOURCE_STANDARD);
        unit.setToleranceAmount(new BigDecimal("0.01"));
        unit.setSlaDays(2);
        unitRepository.save(unit);

        // 2. 대사 실행
        LocalDate reconDate = LocalDate.of(2024, 3, 31);
        ReconciliationResult result = reconManagerService.performDeepReconciliation("UNIT-LOAN-001", reconDate,
                "MANAGER_A");

        // 3. 검증
        assertNotNull(result);
        assertEquals(ReconciliationStatus.VARIANCE_FOUND, result.getStatus());
        assertTrue(result.getVarianceAmount().compareTo(BigDecimal.ZERO) > 0);
    }
}
