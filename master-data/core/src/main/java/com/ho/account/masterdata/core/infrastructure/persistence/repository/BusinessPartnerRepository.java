package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessPartnerRepository extends JpaRepository<BusinessPartner, Long> {

    @Query("""
            SELECT bp FROM BusinessPartner bp
            WHERE bp.businessPartnerCode = :businessPartnerCode
              AND bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            ORDER BY bp.validFrom DESC
            """)
    Optional<BusinessPartner> findActiveByBusinessPartnerCode(String businessPartnerCode, LocalDate date);

    // 운영 PostgreSQL에서는 business_partner_code와 날짜 범위에 exclusion constraint를 추가해야 합니다.
    // 완료 조건: 겹치는 SCD2 행 저장 자체를 거부하는 forward migration과 PostgreSQL 통합 테스트를 함께 둡니다.
    @Query("""
            SELECT bp FROM BusinessPartner bp
            WHERE bp.businessPartnerCode = :businessPartnerCode
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            ORDER BY bp.validFrom DESC
            """)
    Optional<BusinessPartner> findEffectiveByBusinessPartnerCode(String businessPartnerCode, LocalDate date);

    default Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode) {
        return findActiveByBusinessPartnerCode(businessPartnerCode, LocalDate.now());
    }

    @Query("""
            SELECT bp FROM BusinessPartner bp
            WHERE bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            """)
    List<BusinessPartner> findActiveBusinessPartners(LocalDate date);

    default List<BusinessPartner> findByUseYnTrue() {
        return findActiveBusinessPartners(LocalDate.now());
    }

    default boolean existsByBusinessPartnerCode(String businessPartnerCode) {
        return findByBusinessPartnerCode(businessPartnerCode).isPresent();
    }

    @Query("""
            SELECT bp FROM BusinessPartner bp
            WHERE bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
              AND LOWER(bp.businessPartnerName) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY bp.businessPartnerCode, bp.validFrom
            """)
    List<BusinessPartner> searchActiveByName(String name, LocalDate date);

    long countByBusinessPartnerCode(String businessPartnerCode);

    @Query("""
            SELECT COUNT(bp) FROM BusinessPartner bp
            WHERE bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            """)
    long countActiveAt(LocalDate date);
}
