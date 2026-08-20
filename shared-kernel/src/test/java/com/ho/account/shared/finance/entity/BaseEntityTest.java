package com.ho.account.shared.finance.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

    static class TestEntity extends BaseEntity {
    }

    @Test
    @DisplayName("BaseEntity의 감사 필드 getter and setter가 올바르게 동작한다")
    void testBaseEntityGettersAndSetters() {
        TestEntity entity = new TestEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setCreatedAt(now);
        entity.setUpdatedAt(now.plusHours(1));
        entity.setCreatedBy("admin_user");
        entity.setUpdatedBy("operator_user");

        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now.plusHours(1));
        assertThat(entity.getCreatedBy()).isEqualTo("admin_user");
        assertThat(entity.getUpdatedBy()).isEqualTo("operator_user");
    }
}
