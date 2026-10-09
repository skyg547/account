package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.application.port.out.MasterDataBusinessKeyLockPort;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.MasterDataType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정확한 (유형, 업무 키) 행을 현재 트랜잭션이 끝날 때까지 잠급니다.
 * 해시나 JVM 상태를 사용하지 않아 여러 서버와 최초 CREATE도 같은 DB 잠금을 공유합니다.
 */
@Component
public class JpaMasterDataBusinessKeyLockAdapter implements MasterDataBusinessKeyLockPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(MasterDataType targetType, String targetKey) {
        if (targetType == null || targetKey == null || targetKey.isBlank() || targetKey.length() > 100) {
            throw new IllegalArgumentException("A supported master-data type and business key are required.");
        }
        switch (targetType) {
            case ACCOUNT_SUBJECT, BUSINESS_PARTNER, DEPARTMENT, PRODUCT -> {
                // 지원하는 네 SCD2 유형만 잠금 대상으로 허용합니다.
            }
            default -> throw new IllegalArgumentException("Business-key locking is unavailable for this target type.");
        }

        // JPA와 같은 연결을 사용해 잠금 획득과 실제 쓰기의 commit/rollback 경계를 일치시킵니다.
        // 네이티브 JPA query의 자동 flush 없이 먼저 키를 잠근 뒤 서비스가 최신 버전을 읽습니다.
        entityManager.unwrap(Session.class).doWork(connection -> acquire(connection, targetType.name(), targetKey));
    }

    private void acquire(Connection connection, String targetType, String targetKey) throws SQLException {
        String insert = switch (connection.getMetaData().getDatabaseProductName()) {
            case "PostgreSQL" -> """
                    INSERT INTO master_data_business_key_locks (target_type, target_key)
                    VALUES (?, ?) ON CONFLICT (target_type, target_key) DO NOTHING
                    """;
            case "H2" -> """
                    MERGE INTO master_data_business_key_locks (target_type, target_key)
                    VALUES (?, ?)
                    """;
            default -> throw new IllegalStateException("Unsupported database for master-data business-key locking.");
        };

        // 충돌을 예외로 잡는 INSERT는 PostgreSQL 트랜잭션을 실패 상태로 만들므로 사용하지 않습니다.
        // H2는 기본 PK를 사용해 Flyway와 JPA schema 생성의 복합키 컬럼 순서 차이도 수용합니다.
        // 성공한 키 행은 보존해야 최초 키와 기존 키가 항상 같은 잠금 대상을 공유합니다.
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            bindKey(statement, targetType, targetKey);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT target_key FROM master_data_business_key_locks
                WHERE target_type = ? AND target_key = ? FOR UPDATE
                """)) {
            bindKey(statement, targetType, targetKey);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Master-data business-key lock row is missing.");
                }
            }
        }
    }

    private void bindKey(PreparedStatement statement, String targetType, String targetKey) throws SQLException {
        statement.setString(1, targetType);
        statement.setString(2, targetKey);
    }
}
