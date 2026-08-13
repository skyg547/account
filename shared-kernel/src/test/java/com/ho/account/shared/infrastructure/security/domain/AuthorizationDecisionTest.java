package com.ho.account.shared.infrastructure.security.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 권한 판단 결과 값 객체의 데이터 스코프 정규화 규칙을 검증합니다.
 */
class AuthorizationDecisionTest {

    @Test
    void grantedCarriesSuppliedScopeAfterTrimming() {
        AuthorizationDecision decision = AuthorizationDecision.granted("  DEPT_01  ");

        assertThat(decision.granted()).isTrue();
        assertThat(decision.reason()).isEqualTo("GRANTED");
        assertThat(decision.dataScope()).isEqualTo("DEPT_01");
    }

    @Test
    void grantedFallsBackToGlobalScopeWhenScopeMissing() {
        assertThat(AuthorizationDecision.granted(null).dataScope()).isEqualTo("GLOBAL");
        assertThat(AuthorizationDecision.granted("").dataScope()).isEqualTo("GLOBAL");
        assertThat(AuthorizationDecision.granted("   ").dataScope()).isEqualTo("GLOBAL");
    }

    @Test
    void deniedKeepsReasonAndBlocksScope() {
        AuthorizationDecision decision = AuthorizationDecision.denied("NO_MATCHING_GRANT");

        assertThat(decision.granted()).isFalse();
        assertThat(decision.reason()).isEqualTo("NO_MATCHING_GRANT");
        assertThat(decision.dataScope()).isEqualTo("NONE");
    }

    @Test
    void deniedNeverInheritsGlobalScope() {
        assertThat(AuthorizationDecision.denied("EXPIRED").dataScope()).isEqualTo("NONE");
    }
}
