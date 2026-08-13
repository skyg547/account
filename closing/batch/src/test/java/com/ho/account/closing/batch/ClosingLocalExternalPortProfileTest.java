package com.ho.account.closing.batch;

import com.ho.account.closing.infrastructure.local.ClosingLocalExternalPortConfiguration;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClosingLocalExternalPortProfileTest {

    @Test
    void registersJournalPortsForLocalProfile() {
        try (AnnotationConfigApplicationContext context = contextWithProfile("local")) {
            assertThat(context.getBeansOfType(JournalPostingPort.class))
                    .containsOnlyKeys("closingLocalJournalPostingPort");
            assertThat(context.getBeansOfType(JournalQueryPort.class))
                    .containsOnlyKeys("closingLocalJournalQueryPort");
        }
    }

    @Test
    void doesNotRegisterJournalPortsOutsideLocalProfile() {
        try (AnnotationConfigApplicationContext context = contextWithProfile("dev")) {
            assertThat(context.getBeansOfType(JournalPostingPort.class)).isEmpty();
            assertThat(context.getBeansOfType(JournalQueryPort.class)).isEmpty();
        }
    }

    @Test
    void backsOffWhenJournalPortsAreAlreadyProvided() {
        JournalPostingPort approvedPostingPort = mock(JournalPostingPort.class);
        JournalQueryPort approvedQueryPort = mock(JournalQueryPort.class);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local");
            context.registerBean("approvedJournalPostingPort", JournalPostingPort.class, () -> approvedPostingPort);
            context.registerBean("approvedJournalQueryPort", JournalQueryPort.class, () -> approvedQueryPort);
            context.register(ClosingLocalExternalPortConfiguration.class);
            context.refresh();

            assertThat(context.getBeansOfType(JournalPostingPort.class))
                    .containsOnlyKeys("approvedJournalPostingPort")
                    .containsValue(approvedPostingPort);
            assertThat(context.getBeansOfType(JournalQueryPort.class))
                    .containsOnlyKeys("approvedJournalQueryPort")
                    .containsValue(approvedQueryPort);
        }
    }

    private AnnotationConfigApplicationContext contextWithProfile(String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.register(ClosingLocalExternalPortConfiguration.class);
        context.refresh();
        return context;
    }
}
