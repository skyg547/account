package com.ho.account.reconciliation.service;

import com.ho.account.reconciliation.application.port.in.ReconciliationUnitCommand;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import com.ho.account.reconciliation.repository.ReconciliationUnitRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationUnitServiceTest {

    @Mock
    private ReconciliationUnitRepository reconciliationUnitRepository;

    private ReconciliationUnitService reconciliationUnitService;

    @BeforeEach
    void setUp() {
        reconciliationUnitService = new ReconciliationUnitService(reconciliationUnitRepository);
    }

    @Test
    @DisplayName("대사 단위 생성 테스트")
    void createReconciliationUnit() {
        ReconciliationUnitCommand command = new ReconciliationUnitCommand(
                "GL Unit", "GL Daily Recon", ReconciliationUnit.ReconciliationFrequency.DAILY, ReconciliationUnit.ReconciliationType.BANK_BOOK, "{}", true
        );
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setName(command.name());

        when(reconciliationUnitRepository.save(any())).thenReturn(unit);

        ReconciliationUnit result = reconciliationUnitService.createReconciliationUnit(command);

        assertThat(result.getName()).isEqualTo("GL Unit");
        verify(reconciliationUnitRepository).save(any());
    }

    @Test
    @DisplayName("대사 단위 단건 조회 실패 시 EntityNotFoundException 예외 발생")
    void findReconciliationUnitById_NotFound() {
        when(reconciliationUnitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reconciliationUnitService.findReconciliationUnitById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ReconciliationUnit not found with id: 99");
    }

    @Test
    @DisplayName("대사 단위 비활성화(Soft Delete) 테스트")
    void deleteReconciliationUnit_SoftDelete() {
        ReconciliationUnit unit = new ReconciliationUnit();
        unit.setActive(true);

        when(reconciliationUnitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(reconciliationUnitRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationUnitService.deleteReconciliationUnit(1L);

        assertThat(unit.isActive()).isFalse();
        verify(reconciliationUnitRepository).save(unit);
    }
}
