package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.BusinessPartnerJpaEntity;
import java.time.LocalDate;
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

    // @todo 운영 PostgreSQL에서는 business_partner_code와 날짜 범위에 exclusion constraint를 추가해야 합니다.
    // 완료 조건: 겹치는 SCD2 행 저장 자체를 거부하는 forward migration과 PostgreSQL 통합 테스트를 함께 둡니다.
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

    default boolean existsByBusinessPartnerCode(String businessPartnerCode) {
        return existsActiveByBusinessPartnerCode(businessPartnerCode, LocalDate.now());
    }

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
