package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.BusinessPartnerJpaEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 거래처 JPA 엔티티에만 접근하는 Spring Data 저장소입니다.
 *
 * <p>반환 타입이 도메인 {@code BusinessPartner}가 아니라 {@link BusinessPartnerJpaEntity}인
 * 이유는 Spring Data 프록시와 JPQL을 infrastructure 내부에 가두기 위해서입니다. 도메인 변환은
 * 상위 persistence adapter 한 곳에서 수행합니다.</p>
 */
@Repository
public interface BusinessPartnerRepository extends JpaRepository<BusinessPartnerJpaEntity, Long> {

    @EntityGraph(attributePaths = "accounts")
    @Query("""
            SELECT bp FROM BusinessPartnerJpaEntity bp
            WHERE bp.businessPartnerCode = :businessPartnerCode
              AND bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            ORDER BY bp.validFrom DESC
            """)
    Optional<BusinessPartnerJpaEntity> findActiveByBusinessPartnerCode(
            @Param("businessPartnerCode") String businessPartnerCode,
            @Param("date") LocalDate date);

    @EntityGraph(attributePaths = "accounts")
    @Query("""
            SELECT bp FROM BusinessPartnerJpaEntity bp
            WHERE bp.businessPartnerCode IN :codes
              AND bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            ORDER BY bp.businessPartnerCode, bp.validFrom
            """)
    List<BusinessPartnerJpaEntity> findActiveByBusinessPartnerCodeIn(
            @Param("codes") Collection<String> codes,
            @Param("date") LocalDate date);

    // V9의 PostgreSQL exclusion은 useYn과 무관하게 과거 기준일 조회의 유일성도 보호합니다.
    @EntityGraph(attributePaths = "accounts")
    @Query("""
            SELECT bp FROM BusinessPartnerJpaEntity bp
            WHERE bp.businessPartnerCode = :businessPartnerCode
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            ORDER BY bp.validFrom DESC
            """)
    Optional<BusinessPartnerJpaEntity> findEffectiveByBusinessPartnerCode(
            @Param("businessPartnerCode") String businessPartnerCode,
            @Param("date") LocalDate date);

    default Optional<BusinessPartnerJpaEntity> findByBusinessPartnerCode(String businessPartnerCode) {
        return findActiveByBusinessPartnerCode(businessPartnerCode, LocalDate.now());
    }

    @Override
    @EntityGraph(attributePaths = "accounts")
    Optional<BusinessPartnerJpaEntity> findById(Long id);

    // 업무 키 잠금 전에는 scalar만 조회해 잠금 대기 중 변경된 엔티티가 cache에 남지 않게 합니다.
    @Query("SELECT bp.businessPartnerCode FROM BusinessPartnerJpaEntity bp WHERE bp.id = :id")
    Optional<String> findBusinessKeyById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "accounts")
    List<BusinessPartnerJpaEntity> findAll();

    @EntityGraph(attributePaths = "accounts")
    @Query("""
            SELECT bp FROM BusinessPartnerJpaEntity bp
            WHERE bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            ORDER BY bp.businessPartnerCode, bp.validFrom
            """)
    List<BusinessPartnerJpaEntity> findActiveBusinessPartners(@Param("date") LocalDate date);

    default List<BusinessPartnerJpaEntity> findByUseYnTrue() {
        return findActiveBusinessPartners(LocalDate.now());
    }

    /**
     * 존재 여부만 필요한 경로에서는 aggregate와 계좌를 읽지 않고 DB COUNT 결과만 확인합니다.
     */
    @Query("""
            SELECT CASE WHEN COUNT(bp) > 0 THEN true ELSE false END
            FROM BusinessPartnerJpaEntity bp
            WHERE bp.businessPartnerCode = :businessPartnerCode
              AND bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            """)
    boolean existsActiveByBusinessPartnerCode(
            @Param("businessPartnerCode") String businessPartnerCode,
            @Param("date") LocalDate date);

    /** Checks the business key across all SCD2 rows without loading accounts or filtering by date/useYn. */
    boolean existsByBusinessPartnerCode(String businessPartnerCode);

    @EntityGraph(attributePaths = "accounts")
    @Query("""
            SELECT bp FROM BusinessPartnerJpaEntity bp
            WHERE bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
              AND LOWER(bp.businessPartnerName) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY bp.businessPartnerCode, bp.validFrom
            """)
    List<BusinessPartnerJpaEntity> searchActiveByName(
            @Param("name") String name,
            @Param("date") LocalDate date);

    long countByBusinessPartnerCode(String businessPartnerCode);

    @Query("""
            SELECT COUNT(bp) FROM BusinessPartnerJpaEntity bp
            WHERE bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            """)
    long countActiveAt(@Param("date") LocalDate date);
}
