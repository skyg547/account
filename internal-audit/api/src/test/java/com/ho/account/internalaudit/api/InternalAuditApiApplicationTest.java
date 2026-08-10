package com.ho.account.internalaudit.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.internalaudit.api.adapter.in.web.EvaluationController;
import com.ho.account.internalaudit.api.adapter.in.web.RcmController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class InternalAuditApiApplicationTest {

    @Autowired
    private RcmController rcmController;

    @Autowired
    private EvaluationController evaluationController;

    @Test
    void localH2MigrationAndApiCompositionStartTogether() {
        assertThat(rcmController).isNotNull();
        assertThat(evaluationController).isNotNull();
    }
}
