package com.ho.account.shared.infrastructure.security.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RBAC 내부통제 정책의 SOD(직무분리) 판정과 권한 결정 규칙을 검증합니다.
 */
class AuthorizationPolicyTest {

    private final AuthorizationPolicy policy = new AuthorizationPolicy();

    private static Authorization authorization(String functionCode, AccessType accessType, String dataScope) {
        SystemRole role = SystemRole.create("ROLE_TEST", "테스트 역할", null, "tester");
        return Authorization.grant(role, functionCode, accessType, dataScope, "tester");
    }

    @Nested
    class ValidateGrant {

        @Test
        void rejectsWriteWhenExecuteAlreadyGrantedInSameFunctionGroup() {
            List<Authorization> existing = List.of(
                    authorization("JOURNAL.POST", AccessType.EXECUTE, "GLOBAL"));

            assertThatThrownBy(() -> policy.validateGrant(
                    SystemRole.create("ROLE_A", "역할", null, "tester"),
                    existing,
                    "JOURNAL.CREATE",
                    AccessType.WRITE,
                    "GLOBAL"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SOD conflict")
                    .hasMessageContaining("JOURNAL");
        }

        @Test
        void rejectsExecuteWhenWriteAlreadyGrantedInSameFunctionGroup() {
            List<Authorization> existing = List.of(
                    authorization("JOURNAL.CREATE", AccessType.WRITE, "GLOBAL"));

            assertThatThrownBy(() -> policy.validateGrant(
                    SystemRole.create("ROLE_A", "역할", null, "tester"),
                    existing,
                    "JOURNAL.POST",
                    AccessType.EXECUTE,
                    "GLOBAL"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SOD conflict");
        }

        @Test
        void allowsWriteAndExecuteAcrossDifferentFunctionGroups() {
            List<Authorization> existing = List.of(
                    authorization("CLOSING.RUN", AccessType.EXECUTE, "GLOBAL"));

            policy.validateGrant(
                    SystemRole.create("ROLE_A", "역할", null, "tester"),
                    existing,
                    "JOURNAL.CREATE",
                    AccessType.WRITE,
                    "GLOBAL");
        }

        @Test
        void allowsReadRegardlessOfExistingWriteOrExecute() {
            List<Authorization> existing = List.of(
                    authorization("JOURNAL.CREATE", AccessType.WRITE, "GLOBAL"),
                    authorization("JOURNAL.POST", AccessType.EXECUTE, "GLOBAL"));

            policy.validateGrant(
                    SystemRole.create("ROLE_A", "역할", null, "tester"),
                    existing,
                    "JOURNAL.READ",
                    AccessType.READ,
                    "GLOBAL");
        }

        @Test
        void treatsColonAsFunctionGroupSeparator() {
            List<Authorization> existing = List.of(
                    authorization("JOURNAL:POST", AccessType.EXECUTE, "GLOBAL"));

            assertThatThrownBy(() -> policy.validateGrant(
                    SystemRole.create("ROLE_A", "역할", null, "tester"),
                    existing,
                    "JOURNAL:CREATE",
                    AccessType.WRITE,
                    "GLOBAL"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JOURNAL");
        }

        @Test
        void usesEarliestSeparatorWhenBothDotAndColonPresent() {
            // "JOURNAL:SUB.POST" 와 "JOURNAL:SUB.CREATE" 는 앞선 ':' 기준으로 모두 JOURNAL 그룹이다.
            List<Authorization> existing = List.of(
                    authorization("JOURNAL:SUB.POST", AccessType.EXECUTE, "GLOBAL"));

            assertThatThrownBy(() -> policy.validateGrant(
                    SystemRole.create("ROLE_A", "역할", null, "tester"),
                    existing,
                    "JOURNAL.SUB:CREATE",
                    AccessType.WRITE,
                    "GLOBAL"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void treatsWholeCodeAsGroupWhenNoSeparatorPresent() {
            List<Authorization> existing = List.of(
                    authorization("JOURNAL", AccessType.EXECUTE, "GLOBAL"));

            assertThatThrownBy(() -> policy.validateGrant(
                    SystemRole.create("ROLE_A", "역할", null, "tester"),
                    existing,
                    "JOURNAL",
                    AccessType.WRITE,
                    "GLOBAL"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void acceptsNullOrEmptyExistingAuthorizations() {
            SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

            policy.validateGrant(role, null, "JOURNAL.CREATE", AccessType.WRITE, "GLOBAL");
            policy.validateGrant(role, List.of(), "JOURNAL.CREATE", AccessType.WRITE, "GLOBAL");
        }

        @Test
        void rejectsNullRole() {
            assertThatThrownBy(() -> policy.validateGrant(
                    null, List.of(), "JOURNAL.CREATE", AccessType.WRITE, "GLOBAL"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("role");
        }

        @Test
        void rejectsBlankFunctionCode() {
            SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

            assertThatThrownBy(() -> policy.validateGrant(
                    role, List.of(), "   ", AccessType.WRITE, "GLOBAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Function code is required.");
        }

        @Test
        void rejectsNullAccessType() {
            SystemRole role = SystemRole.create("ROLE_A", "역할", null, "tester");

            assertThatThrownBy(() -> policy.validateGrant(
                    role, List.of(), "JOURNAL.CREATE", null, "GLOBAL"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("accessType");
        }
    }

    @Nested
    class Decide {

        @Test
        void grantsWhenFunctionCodeAndAccessTypeBothMatch() {
            List<Authorization> authorizations = List.of(
                    authorization("JOURNAL.CREATE", AccessType.WRITE, "DEPT_01"));

            AuthorizationDecision decision = policy.decide(authorizations, "JOURNAL.CREATE", AccessType.WRITE);

            assertThat(decision.granted()).isTrue();
            assertThat(decision.reason()).isEqualTo("GRANTED");
            assertThat(decision.dataScope()).isEqualTo("DEPT_01");
        }

        @Test
        void deniesWhenAccessTypeDiffers() {
            List<Authorization> authorizations = List.of(
                    authorization("JOURNAL.CREATE", AccessType.READ, "DEPT_01"));

            AuthorizationDecision decision = policy.decide(authorizations, "JOURNAL.CREATE", AccessType.WRITE);

            assertThat(decision.granted()).isFalse();
            assertThat(decision.reason()).isEqualTo("NO_MATCHING_GRANT");
            assertThat(decision.dataScope()).isEqualTo("NONE");
        }

        @Test
        void deniesWhenFunctionCodeDiffers() {
            List<Authorization> authorizations = List.of(
                    authorization("JOURNAL.CREATE", AccessType.WRITE, "DEPT_01"));

            AuthorizationDecision decision = policy.decide(authorizations, "CLOSING.CREATE", AccessType.WRITE);

            assertThat(decision.granted()).isFalse();
        }

        @Test
        void deniesForNullOrEmptyAuthorizations() {
            assertThat(policy.decide(null, "JOURNAL.CREATE", AccessType.WRITE).granted()).isFalse();
            assertThat(policy.decide(List.of(), "JOURNAL.CREATE", AccessType.WRITE).granted()).isFalse();
        }

        @Test
        void matchesAfterTrimmingRequestedFunctionCode() {
            List<Authorization> authorizations = List.of(
                    authorization("JOURNAL.CREATE", AccessType.WRITE, "DEPT_01"));

            assertThat(policy.decide(authorizations, "  JOURNAL.CREATE  ", AccessType.WRITE).granted()).isTrue();
        }

        @Test
        void returnsFirstMatchWhenMultipleGrantsExist() {
            List<Authorization> authorizations = List.of(
                    authorization("JOURNAL.CREATE", AccessType.WRITE, "DEPT_01"),
                    authorization("JOURNAL.CREATE", AccessType.WRITE, "DEPT_02"));

            assertThat(policy.decide(authorizations, "JOURNAL.CREATE", AccessType.WRITE).dataScope())
                    .isEqualTo("DEPT_01");
        }

        @Test
        void rejectsBlankFunctionCode() {
            assertThatThrownBy(() -> policy.decide(List.of(), " ", AccessType.WRITE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Function code is required.");
        }

        @Test
        void rejectsNullAccessType() {
            assertThatThrownBy(() -> policy.decide(List.of(), "JOURNAL.CREATE", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("accessType");
        }
    }

    @Nested
    class NormalizeDataScope {

        @Test
        void fallsBackToGlobalForNullOrBlank() {
            assertThat(policy.normalizeDataScope(null)).isEqualTo("GLOBAL");
            assertThat(policy.normalizeDataScope("")).isEqualTo("GLOBAL");
            assertThat(policy.normalizeDataScope("   ")).isEqualTo("GLOBAL");
        }

        @Test
        void trimsSuppliedScope() {
            assertThat(policy.normalizeDataScope("  DEPT_01  ")).isEqualTo("DEPT_01");
        }
    }
}
