package com.ho.account.masterdata.core.infrastructure.persistence;

import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest;
import com.ho.account.masterdata.core.domain.changerequest.MasterDataChangeRequest.ChangeStatus;
import com.ho.account.masterdata.core.infrastructure.persistence.entity.MasterDataChangeRequestEntity;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.MasterDataChangeRequestMapper;
import com.ho.account.masterdata.core.application.port.out.MasterDataChangeRequestPersistencePort;
import com.ho.account.masterdata.core.application.query.PageSlice;
import com.ho.account.masterdata.core.application.query.PageWindow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaMasterDataChangeRequestPersistenceAdapter implements MasterDataChangeRequestPersistencePort {

    private final MasterDataChangeRequestJpaRepository repository;
    private final MasterDataChangeRequestMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaMasterDataChangeRequestPersistenceAdapter(
            MasterDataChangeRequestJpaRepository repository,
            MasterDataChangeRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<MasterDataChangeRequest> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<MasterDataChangeRequest> findByIdForUpdate(Long id) {
        return repository.findById(id).map(entity -> {
            // 이전 query의 자동 flush 의미를 보존해 같은 트랜잭션의 승인/반영 저장을 잃지 않습니다.
            entityManager.flush();
            // 기존 @Lock query는 이미 읽은 객체의 오래된 @Version을 refresh 전에 검사할 수 있습니다.
            // refresh 한 번으로 행 잠금과 최신 상태/lockVersion 재조회를 함께 수행합니다.
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return mapper.toDomain(entity);
        });
    }

    @Override
    public Optional<MasterDataChangeRequest> findBySourceReference(String sourceReference) {
        return repository.findBySourceReference(sourceReference).map(mapper::toDomain);
    }

    @Override
    public PageSlice<MasterDataChangeRequest> findByStatus(ChangeStatus status, PageWindow page) {
        var slice = repository.findByStatusOrderByRequestedAtAscIdAsc(
                status, PageRequest.of(page.page(), page.size()));
        return new PageSlice<>(slice.getContent().stream().map(mapper::toDomain).toList(), slice.hasNext());
    }

    @Override
    public List<MasterDataChangeRequest> findReadyToApply(LocalDate effectiveDate, int limit) {
        return repository.findByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateAscRequestedAtAscIdAsc(
                ChangeStatus.APPROVED,
                effectiveDate,
                PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public CreateOrLoadResult createOrLoad(MasterDataChangeRequest request) {
        if (request.getSourceReference() == null) {
            return new CreateOrLoadResult(save(request), true);
        }
        if (request.getId() != null || request.getStatus() != ChangeStatus.REQUESTED) {
            throw new IllegalArgumentException("Only a new requested change can be created idempotently.");
        }

        Session session = entityManager.unwrap(Session.class);
        String database = session.doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());
        if ("H2".equals(database)) {
            // 동일 sourceReference의 최초 저장도 하나의 DB 행 잠금 뒤에 조회합니다.
            // 대상 테이블의 고유키 위반을 catch하면 H2/PostgreSQL 모두 트랜잭션 회복 근거가 약해집니다.
            session.doWork(connection -> lockH2SourceReference(connection, request.getSourceReference()));
            return findBySourceReference(request.getSourceReference())
                    .map(existing -> new CreateOrLoadResult(existing, false))
                    .orElseGet(() -> new CreateOrLoadResult(save(request), true));
        }
        if (!"PostgreSQL".equals(database)) {
            throw new IllegalStateException("Unsupported database for idempotent master-data changes.");
        }

        // JDBC와 JPA가 같은 연결/트랜잭션을 사용합니다. 앞선 JPA 쓰기가 있으면 먼저 반영합니다.
        entityManager.flush();
        InsertOutcome outcome = session.doReturningWork(connection -> insertOrFind(connection, request));
        MasterDataChangeRequest persisted = repository.findById(outcome.id())
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("Idempotent change request row is missing."));
        return new CreateOrLoadResult(persisted, outcome.created());
    }

    private void lockH2SourceReference(Connection connection, String sourceReference) throws SQLException {
        // V8 잠금 테이블의 별도 namespace를 사용하고 긴 원문은 고정 길이 키로 변환합니다.
        String lockKey = sourceReferenceLockKey(sourceReference);
        try (PreparedStatement statement = connection.prepareStatement("""
                MERGE INTO master_data_business_key_locks (target_type, target_key)
                VALUES ('SOURCE_REFERENCE', ?)
                """)) {
            statement.setString(1, lockKey);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT target_key FROM master_data_business_key_locks
                WHERE target_type = 'SOURCE_REFERENCE' AND target_key = ? FOR UPDATE
                """)) {
            statement.setString(1, lockKey);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Source-reference lock row is missing.");
                }
            }
        }
    }

    private String sourceReferenceLockKey(String sourceReference) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sourceReference.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable for source-reference locking.", exception);
        }
    }

    private InsertOutcome insertOrFind(Connection connection, MasterDataChangeRequest request) throws SQLException {
        // 고유키 충돌을 SQL의 정상 결과로 처리해야 PostgreSQL 트랜잭션이 rollback-only가 되지 않습니다.
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO master_data_change_requests (
                    target_type, target_key, change_type, status, lock_version,
                    effective_date, requested_version, requested_by, requested_at,
                    reason, payload_json, source_reference
                ) VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_reference) DO NOTHING
                RETURNING id
                """)) {
            statement.setString(1, request.getTargetType().name());
            statement.setString(2, request.getTargetKey());
            statement.setString(3, request.getChangeType().name());
            statement.setString(4, request.getStatus().name());
            statement.setObject(5, request.getEffectiveDate());
            statement.setInt(6, request.getRequestedVersion());
            statement.setString(7, request.getRequestedBy());
            statement.setObject(8, request.getRequestedAt());
            statement.setString(9, request.getRequestReason());
            statement.setString(10, request.getPayloadJson());
            statement.setString(11, request.getSourceReference());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new InsertOutcome(result.getLong(1), true);
                }
            }
        }

        // READ COMMITTED의 새 문장에서 방금 경쟁 트랜잭션이 커밋한 행을 볼 수 있습니다.
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id FROM master_data_change_requests WHERE source_reference = ?
                """)) {
            statement.setString(1, request.getSourceReference());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new InsertOutcome(result.getLong(1), false);
                }
            }
        }
        throw new IllegalStateException("Idempotent change request requires a visible committed row.");
    }

    private record InsertOutcome(long id, boolean created) { }

    @Override
    public MasterDataChangeRequest save(MasterDataChangeRequest request) {
        MasterDataChangeRequestEntity entity = mapper.toEntity(request);
        MasterDataChangeRequestEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
