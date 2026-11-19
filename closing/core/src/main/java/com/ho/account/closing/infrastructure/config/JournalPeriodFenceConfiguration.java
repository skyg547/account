package com.ho.account.closing.infrastructure.config;

import com.ho.account.contracts.journal.JournalPeriodCheckpoint;
import com.ho.account.contracts.journal.JournalPeriodFenceCommand;
import com.ho.account.contracts.journal.JournalPeriodFencePort;
import com.ho.account.contracts.journal.JournalPeriodFenceResult;
import com.ho.account.contracts.journal.JournalPeriodScope;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Resolves an embedded/remote implementation at invocation time; isolated runtimes fail closed. */
@Configuration(proxyBeanMethods = false)
public class JournalPeriodFenceConfiguration {
    @Bean("closingJournalPeriodFence")
    public JournalPeriodFencePort closingJournalPeriodFence(ObjectProvider<JournalPeriodFencePort> candidates) {
        return new JournalPeriodFencePort() {
            private JournalPeriodFencePort delegate() {
                var ports = candidates.stream().filter(port -> port != this).toList();
                if (ports.size() != 1) {
                    throw new IllegalStateException("Exactly one authoritative Journal period fence adapter is required");
                }
                return ports.get(0);
            }
            public JournalPeriodCheckpoint checkpoint(JournalPeriodScope scope) { return delegate().checkpoint(scope); }
            public JournalPeriodFenceResult fence(JournalPeriodFenceCommand command) { return delegate().fence(command); }
            public JournalPeriodFenceResult release(JournalPeriodFenceCommand command) { return delegate().release(command); }
        };
    }
}
