package com.ho.account.deposit.infrastructure.config;

import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.outbox.JournalOutboxRelayService;
import com.ho.account.contracts.outbox.OutboxEventPublisher;
import com.ho.account.contracts.outbox.OutboxPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [예금 아웃박스 빈 설정 (DepositOutboxConfiguration)]
 *
 * 🐣 [초보자를 위한 설명]
 * OutboxPort와 JournalPostingPort를 주입받아 OutboxEventPublisher (JournalOutboxRelayService) 빈을 등록합니다.
 */
@Configuration
public class DepositOutboxConfiguration {

    @Bean
    @ConditionalOnMissingBean(OutboxEventPublisher.class)
    public OutboxEventPublisher journalOutboxRelayService(
            OutboxPort outboxPort,
            JournalPostingPort journalPostingPort) {
        return new JournalOutboxRelayService(outboxPort, journalPostingPort);
    }
}
