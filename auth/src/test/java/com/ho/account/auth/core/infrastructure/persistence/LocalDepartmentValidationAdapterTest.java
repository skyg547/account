package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalDepartmentValidationAdapterTest {

    private final LocalDepartmentValidationAdapter adapter = new LocalDepartmentValidationAdapter();

    @Test
    @DisplayName("로컬 어댑터는 유효한 부서 코드에 대해 항상 true를 반환한다")
    void existsDepartmentCode_validCode_returnsTrue() {
        boolean exists = adapter.existsDepartmentCode("FIN");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("null 이거나 빈 문자열 부서 코드는 false를 반환한다")
    void existsDepartmentCode_nullOrBlank_returnsFalse() {
        assertThat(adapter.existsDepartmentCode(null)).isFalse();
        assertThat(adapter.existsDepartmentCode("")).isFalse();
        assertThat(adapter.existsDepartmentCode("   ")).isFalse();
    }
}
