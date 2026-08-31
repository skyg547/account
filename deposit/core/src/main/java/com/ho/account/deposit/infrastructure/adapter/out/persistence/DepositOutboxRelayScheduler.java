package com.ho.account.deposit.infrastructure.adapter.out.persistence;

import com.ho.account.contracts.outbox.OutboxEventPublisher;
import com.ho.account.contracts.outbox.OutboxStatus;
import com.ho.account.deposit.domain.DepositOutboxEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * [예금 아웃박스 릴레이 스케줄러 (DepositOutboxRelayScheduler)]
 *
 * 🐣 [초보자를 위한 설명]
 * 주기적으로 deposit_outbox 테이블의 PENDING 상태 이벤트를 폴링하여
 * JournalPostingPort로 릴레이(전달)하고, 통신 장애로 FAILED된 이벤트를 재시도할 수 있도록 지원합니다.
 */
@Component
@ConditionalOnProperty(prefix = "account.deposit.outbox.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DepositOutboxRelayScheduler {

    private static final Logger log = Logger.getLogger(DepositOutboxRelayScheduler.class.getName());

    private final OutboxEventPublisher outboxEventPublisher;
    private final SpringDataDepositOutboxRepository outboxRepository;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Autowired
    public DepositOutboxRelayScheduler(OutboxEventPublisher outboxEventPublisher,
                                      SpringDataDepositOutboxRepository outboxRepository) {
        this.outboxEventPublisher = outboxEventPublisher;
        this.outboxRepository = outboxRepository;
    }

    /**
     * 주기적으로 PENDING 상태의 전표 Outbox 이벤트를 폴링하여 외부로 릴레이합니다.
     */
    @Scheduled(fixedDelayString = "${account.deposit.outbox.relay-interval-ms:5000}")
    public void scheduleRelay() {
        if (!isRunning.compareAndSet(false, true)) {
            log.fine("[DepositOutboxRelayScheduler] Another relay cycle is in progress. Skipping.");
            return;
        }

        try {
            int count = relayPendingEvents();
            if (count > 0) {
                log.info(() -> String.format("[DepositOutboxRelayScheduler] Successfully relayed %d pending journal outbox events.", count));
            }
        } catch (Exception e) {
            log.warning(() -> String.format("[DepositOutboxRelayScheduler] Error occurred during outbox relay: %s", e.getMessage()));
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * PENDING 상태의 전표 Outbox 이벤트를 수동/동기 폴링 및 발행 처리합니다.
     */
    public int relayPendingEvents() {
        return outboxEventPublisher.publishPendingJournalEvents();
    }

    /**
     * 최대 재시도 초과로 FAILED 상태로 전환된 Outbox 이벤트를 PENDING으로 리셋하여 재시도 대기 상태로 복구합니다.
     */
    @Transactional
    public int retryFailedEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit > 0 ? limit : 100);
        List<DepositOutboxEntity> failedEntities = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.FAILED,
                pageable
        );

        int resetCount = 0;
        for (DepositOutboxEntity entity : failedEntities) {
            entity.setStatus(OutboxStatus.PENDING);
            entity.setRetryCount(0);
            entity.setErrorMessage(null);
            outboxRepository.save(entity);
            resetCount++;
        }

        if (resetCount > 0) {
            final int count = resetCount;
            log.info(() -> String.format("[DepositOutboxRelayScheduler] Reset %d FAILED outbox events to PENDING for retry.", count));
        }
        return resetCount;
    }
}
