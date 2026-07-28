package com.ho.account.security.domain;

import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SystemUserTest {

    @Test
    void storesDepartmentAsIdReferenceOnly() {
        SystemUser user = new SystemUser();
        user.setDepartmentId(100L);

        assertThat(user.getDepartmentId()).isEqualTo(100L);
        assertThat(Arrays.stream(SystemUser.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ManyToOne.class))
                .map(Field::getName))
                .doesNotContain("department");
    }
}
