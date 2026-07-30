package com.ho.account.budget.api.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Verifies Auth-issued HS256 JWTs at the Budget service boundary.
 *
 * <p>Gateway headers are intentionally not trusted: a client that reaches this
 * service directly must still present a correctly signed, unexpired token from
 * {@code auth-service}. Controllers can therefore use JWT {@code sub} as the
 * audited actor instead of accepting a spoofable JSON field.</p>
 */
@Configuration
public class BudgetApiSecurityConfiguration {

    @Bean
    SecurityFilterChain budgetSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/budgets/plans/*/approval",
                                "/api/budgets/transfers/*/approval")
                        .hasAnyAuthority(
                                "ROLE_BUDGET_APPROVER",
                                "ROLE_ACCOUNTING_ADMIN",
                                "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/budgets/**")
                        .hasAnyAuthority(
                                "ROLE_BUDGET_MANAGER",
                                "ROLE_ACCOUNTING_ADMIN",
                                "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/budgets/**")
                        .authenticated()
                        .requestMatchers("/api/budgets/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtDecoder budgetJwtDecoder(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.issuer:auth-service}") String issuer) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("auth.jwt.secret must be at least 32 bytes for HS256");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secretBytes, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(issuer)));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        // Auth already emits canonical ROLE_* values. Adding a prefix here
        // would silently turn ROLE_ADMIN into ROLE_ROLE_ADMIN.
        roles.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
