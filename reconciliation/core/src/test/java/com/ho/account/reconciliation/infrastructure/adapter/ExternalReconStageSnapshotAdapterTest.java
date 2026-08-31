package com.ho.account.reconciliation.infrastructure.adapter;

import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshot;
import com.ho.account.reconciliation.application.port.out.ExternalReconSnapshotRequest;
import com.ho.account.reconciliation.infrastructure.persistence.ExternalReconStageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalReconStageSnapshotAdapterTest {

    @Mock
    private ExternalReconStageRecordRepository repository;

    private ExternalReconStageSnapshotAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ExternalReconStageSnapshotAdapter(repository);
    }

    @Test
    void loadSnapshotReturnsRepositoryAggregate() {
        LocalDate reconDate = LocalDate.of(2026, 5, 11);
        ExternalReconSnapshotRequest request = ExternalReconSnapshotRequest.of(
                "UNIT-001",
                "SOURCE",
                reconDate,
                "LOAN",
                "KRW",
                "HO"
        );
        when(repository.summarize("UNIT-001", "SOURCE", reconDate, "LOAN", "KRW", "HO"))
                .thenReturn(projection(12L, new BigDecimal("1234.56")));

        ExternalReconSnapshot snapshot = adapter.loadSnapshot(request);

        assertThat(snapshot.count()).isEqualTo(12L);
        assertThat(snapshot.amount()).isEqualByComparingTo("1234.56");
        verify(repository).summarize("UNIT-001", "SOURCE", reconDate, "LOAN", "KRW", "HO");
    }

    @Test
    void loadSnapshotReturnsZeroWhenRepositoryHasNoProjection() {
        LocalDate reconDate = LocalDate.of(2026, 5, 11);
        ExternalReconSnapshotRequest request = ExternalReconSnapshotRequest.of(
                "UNIT-001",
                "INTERFACE",
                reconDate,
                null,
                null,
                null
        );
        when(repository.summarize("UNIT-001", "INTERFACE", reconDate, "", "", ""))
                .thenReturn(null);

        ExternalReconSnapshot snapshot = adapter.loadSnapshot(request);

        assertThat(snapshot.count()).isZero();
        assertThat(snapshot.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private ExternalReconStageRecordRepository.SnapshotAggregateProjection projection(Long count, BigDecimal amount) {
        return new ExternalReconStageRecordRepository.SnapshotAggregateProjection() {
            @Override
            public Long getItemCount() {
                return count;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return amount;
            }
        };
    }
}
