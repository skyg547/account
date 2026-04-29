package com.ho.account.audit.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.audit.application.model.AuditActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RequestAuditActorAdapterTest {

    private final RequestAuditActorAdapter adapter = new RequestAuditActorAdapter();

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void currentActor_usesUserHeaderAndForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-ID", "tester");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AuditActor actor = adapter.currentActor();

        assertThat(actor.userId()).isEqualTo("tester");
        assertThat(actor.ipAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void currentActor_returnsSystemDefaultsWithoutRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        AuditActor actor = adapter.currentActor();

        assertThat(actor.userId()).isEqualTo("SYSTEM");
        assertThat(actor.ipAddress()).isEqualTo("0.0.0.0");
    }
}

