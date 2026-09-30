package com.ho.account.masterdata.core.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * 업무 행이 아직 없는 CREATE도 같은 잠금 대상을 갖도록 보존하는 업무 키입니다.
 * 실제 잠금은 JDBC 어댑터가 수행하며, 이 매핑은 JPA validate와 H2 schema 생성에도 사용됩니다.
 */
@Entity
@Table(name = "master_data_business_key_locks")
public class MasterDataBusinessKeyLockEntity {

    @EmbeddedId
    private BusinessKey id;

    protected MasterDataBusinessKeyLockEntity() {
    }

    @Embeddable
    public static class BusinessKey implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "target_type", nullable = false, length = 40)
        private String targetType;

        @Column(name = "target_key", nullable = false, length = 100)
        private String targetKey;

        public BusinessKey() {
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof BusinessKey that)) {
                return false;
            }
            return Objects.equals(targetType, that.targetType)
                    && Objects.equals(targetKey, that.targetKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetType, targetKey);
        }
    }
}
