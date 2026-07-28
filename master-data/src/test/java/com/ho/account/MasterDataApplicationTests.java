package com.ho.account;

import com.ho.account.masterdata.core.application.port.in.MasterDataChangeRequestUseCase;
import com.ho.account.masterdata.core.application.port.out.MasterDataVersionQueryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=local",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
class MasterDataApplicationTests {

    @Autowired
    private MasterDataChangeRequestUseCase changeRequestUseCase;

    @Autowired
    private MasterDataVersionQueryPort versionQueryPort;

    @Test
    void loadsControlledChangeWorkflowWithVersionAdapter() {
        assertThat(changeRequestUseCase).isNotNull();
        assertThat(versionQueryPort).isNotNull();
    }
}