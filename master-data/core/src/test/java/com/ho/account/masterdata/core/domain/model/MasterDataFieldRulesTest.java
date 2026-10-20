package com.ho.account.masterdata.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Partner child rows have their own persisted column limits, including direct-domain callers. */
class MasterDataFieldRulesTest {

    @ParameterizedTest(name = "{0} max {1}")
    @MethodSource("accountFields")
    void businessPartnerAccountAcceptsExactlyColumnMaximum(String field, int maximum) {
        BusinessPartnerAccount account = accountWith(field, "X".repeat(maximum));

        assertThat(valueOf(account, field)).hasSize(maximum);
    }

    @ParameterizedTest(name = "{0} max+1 {1}")
    @MethodSource("accountFields")
    void businessPartnerAccountRejectsColumnOverflow(String field, int maximum) {
        String oversized = "X".repeat(maximum + 1);

        assertThatThrownBy(() -> accountWith(field, oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining(oversized);
    }

    private static Stream<Arguments> accountFields() {
        return Stream.of(
                Arguments.of("bankName", 100),
                Arguments.of("accountNumber", 100),
                Arguments.of("accountHolder", 100),
                Arguments.of("swiftCode", 20));
    }

    private static BusinessPartnerAccount accountWith(String field, String value) {
        return BusinessPartnerAccount.create(
                field.equals("bankName") ? value : "Bank",
                field.equals("accountNumber") ? value : "1234",
                field.equals("accountHolder") ? value : "Holder",
                field.equals("swiftCode") ? value : "ABCDEFGH",
                false);
    }

    private static String valueOf(BusinessPartnerAccount account, String field) {
        return switch (field) {
            case "bankName" -> account.getBankName();
            case "accountNumber" -> account.getAccountNumber();
            case "accountHolder" -> account.getAccountHolder();
            case "swiftCode" -> account.getSwiftCode();
            default -> throw new IllegalArgumentException("Unexpected field: " + field);
        };
    }
}
