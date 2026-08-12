package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.application.port.in.ReconciliationRuleCommand;
import com.ho.account.reconciliation.domain.DifferenceReasonCode;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.DifferenceReasonCodeRepository;
import com.ho.account.reconciliation.repository.ReconciliationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationRuleServiceTest {

    @Mock
    private ReconciliationRuleRepository reconciliationRuleRepository;

    @Mock
    private DifferenceReasonCodeRepository differenceReasonCodeRepository;

    @Mock
    private ReconciliationUnitService reconciliationUnitService;

    private ReconciliationRuleService reconciliationRuleService;

    @BeforeEach
    void setUp() {
        reconciliationRuleService = new ReconciliationRuleService(
                reconciliationRuleRepository,
                differenceReasonCodeRepository,
                reconciliationUnitService
        );
    }

    @Test
    @DisplayName("대사 규칙 생성 테스트")
    void createReconciliationRule() {
        ReconciliationUnit unit = new ReconciliationUnit();
        when(reconciliationUnitService.findReconciliationUnitById(1L)).thenReturn(unit);

        ReconciliationRuleCommand command = new ReconciliationRuleCommand(
                1L, "Default Rule", "{}", ReconciliationRule.ToleranceType.ABSOLUTE, new BigDecimal("10.00"), 1, true
        );
        ReconciliationRule rule = new ReconciliationRule();
        rule.setName("Default Rule");

        when(reconciliationRuleRepository.save(any())).thenReturn(rule);

        ReconciliationRule result = reconciliationRuleService.createReconciliationRule(command);

        assertThat(result.getName()).isEqualTo("Default Rule");
        verify(reconciliationRuleRepository).save(any());
    }

    @Test
    @DisplayName("차액 사유 코드 비활성화(Soft Delete) 테스트")
    void deleteDifferenceReasonCode_SoftDelete() {
        DifferenceReasonCode reasonCode = new DifferenceReasonCode();
        reasonCode.setActive(true);

        when(differenceReasonCodeRepository.findById(100L)).thenReturn(Optional.of(reasonCode));
        when(differenceReasonCodeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationRuleService.deleteDifferenceReasonCode(100L);

        assertThat(reasonCode.isActive()).isFalse();
        verify(differenceReasonCodeRepository).save(reasonCode);
    }
}
