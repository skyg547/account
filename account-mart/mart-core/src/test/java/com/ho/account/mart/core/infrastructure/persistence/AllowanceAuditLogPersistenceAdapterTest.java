package com.ho.account.mart.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.mart.core.domain.governance.AllowanceAuditLog;
import com.ho.account.mart.core.infrastructure.persistence.entity.governance.AllowanceAuditLogEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaAllowanceAuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AllowanceAuditLogPersistenceAdapterTest {

    private final JpaAllowanceAuditLogRepository jpaRepository = org.mockito.Mockito.mock(JpaAllowanceAuditLogRepository.class);
    private final AllowanceAuditLogPersistenceAdapter adapter = new AllowanceAuditLogPersistenceAdapter(jpaRepository);

    @Test
    void saveFillsCreatedAtWhenMissing() {
        when(jpaRepository.save(any(AllowanceAuditLogEntity.class))).thenAnswer(invocation -> {
            AllowanceAuditLogEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        AllowanceAuditLog saved = adapter.save(AllowanceAuditLog.builder()
                .serviceName("IFRS9_ALLOWANCE")
                .actionType("ALLOWANCE_ECL_JOB")
                .status("SUCCESS")
                .executedBy("tester")
                .durationMs(100L)
                .build());

        ArgumentCaptor<AllowanceAuditLogEntity> captor = ArgumentCaptor.forClass(AllowanceAuditLogEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findRecentLogsMapsEntitymain() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 30, 12, 0);
        when(jpaRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(AllowanceAuditLogEntity.builder()
                .id(1L)
                .serviceName("MART")
                .actionType("SNAPSHOT")
                .status("SUCCESS")
                .createdAt(createdAt)
                .durationMs(50L)
                .build()));

        List<AllowanceAuditLog> logs = adapter.findTop10ByOrderByCreatedAtDesc();

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getServiceName()).isEqualTo("MART");
        assertThat(logs.get(0).getCreatedAt()).isEqualTo(createdAt);
    }
}
