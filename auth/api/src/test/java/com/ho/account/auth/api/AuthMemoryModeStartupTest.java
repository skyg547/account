package com.ho.account.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.api.web.PersonalAccessTokenController;
import com.ho.account.auth.core.application.port.in.PersonalAccessTokenUseCase;
import com.ho.account.auth.core.application.port.out.PersonalAccessTokenPort;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Verifies the complete local memory-mode bean graph and its PAT HTTP contract. */
@SpringBootTest(
        classes = AuthApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:auth-memory-mode-startup;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "auth.persistence.mode=memory",
                "auth.master-data.enabled=false",
                "auth.login-security.store=memory"
        })
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthMemoryModeStartupTest {
    private static final String JWT_SECRET = randomSecret();
    private static final String INTERNAL_TOKEN = randomSecret();

    @Autowired private ApplicationContext context;
    @Autowired private MockMvc mockMvc;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping mappings;

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> JWT_SECRET);
        registry.add("auth.internal-api.token", () -> INTERNAL_TOKEN);
    }

    @Test
    void fullMemoryContextStartsWithoutPatRoutes() throws Exception {
        assertThat(context.getBeansOfType(PersonalAccessTokenController.class)).isEmpty();
        assertThat(context.getBeansOfType(PersonalAccessTokenUseCase.class)).isEmpty();
        assertThat(context.getBeansOfType(PersonalAccessTokenPort.class)).isEmpty();
        assertThat(mappings.getHandlerMethods().values())
                .noneMatch(handler -> handler.getBeanType().equals(PersonalAccessTokenController.class));
        mockMvc.perform(get("/api/auth/pat")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/auth/admin/pat")).andExpect(status().isNotFound());
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
