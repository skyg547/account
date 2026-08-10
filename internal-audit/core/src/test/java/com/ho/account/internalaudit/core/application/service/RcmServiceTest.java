package com.ho.account.internalaudit.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.internalaudit.core.application.port.out.RcmPersistencePort;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RcmServiceTest {

    @Mock
    private RcmPersistencePort persistencePort;

    private RcmService service;

    @BeforeEach
    void setUp() {
        service = new RcmService(persistencePort);
    }

    @Test
    void pathProcessIdIsAuthoritativeWhenRiskBodyOmitsIt() {
        when(persistencePort.findProcessById("process-a"))
                .thenReturn(Optional.of(new RcmProcess("process-a", "Process", null, null)));
        RcmRisk command = new RcmRisk("risk-a", null, "Risk", "HIGH", "LIKELY");
        when(persistencePort.saveRisk(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RcmRisk saved = service.addRisk("process-a", command);

        assertThat(saved.processId()).isEqualTo("process-a");
        ArgumentCaptor<RcmRisk> captor = ArgumentCaptor.forClass(RcmRisk.class);
        verify(persistencePort).saveRisk(captor.capture());
        assertThat(captor.getValue().processId()).isEqualTo("process-a");
    }

    @Test
    void mismatchedRiskParentIsRejectedBeforePersistence() {
        RcmRisk command = new RcmRisk("risk-a", "process-b", "Risk", "HIGH", "LIKELY");

        assertThatThrownBy(() -> service.addRisk("process-a", command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("processId");
        verify(persistencePort, never()).saveRisk(any());
    }

    @Test
    void missingRiskParentIsRejectedBeforePersistence() {
        when(persistencePort.findProcessById("missing")).thenReturn(Optional.empty());
        RcmRisk command = new RcmRisk("risk-a", null, "Risk", "HIGH", "LIKELY");

        assertThatThrownBy(() -> service.addRisk("missing", command))
                .isInstanceOf(NoSuchElementException.class);
        verify(persistencePort, never()).saveRisk(any());
    }

    @Test
    void pathRiskIdIsAuthoritativeWhenControlBodyOmitsIt() {
        when(persistencePort.findRiskById("risk-a"))
                .thenReturn(Optional.of(new RcmRisk("risk-a", "process-a", null, null, null)));
        ControlActivity command = new ControlActivity(
                "control-a", null, "Control", "PREVENTIVE", "MANUAL", "DAILY", "owner");
        when(persistencePort.saveControlActivity(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ControlActivity saved = service.addControl("risk-a", command);

        assertThat(saved.riskId()).isEqualTo("risk-a");
    }

    @Test
    void mismatchedControlParentIsRejectedBeforePersistence() {
        ControlActivity command = new ControlActivity(
                "control-a", "risk-b", "Control", "PREVENTIVE", "MANUAL", "DAILY", "owner");

        assertThatThrownBy(() -> service.addControl("risk-a", command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskId");
        verify(persistencePort, never()).saveControlActivity(any());
    }

    @Test
    void identifiersAreRequiredBeforePersistence() {
        RcmRisk command = new RcmRisk(" ", null, "Risk", "HIGH", "LIKELY");

        assertThatThrownBy(() -> service.addRisk("process-a", command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskId");
        verify(persistencePort, never()).saveRisk(any());
    }
}
