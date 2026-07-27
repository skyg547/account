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
    List<BusinessPartner> findActiveByBusinessPartnerCode(String businessPartnerCode, LocalDate date);

    default Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode) {
        return findActiveByBusinessPartnerCode(businessPartnerCode, LocalDate.now()).stream().findFirst();
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

    List<BusinessPartner> findByBusinessPartnerNameContaining(String name);

    long countByBusinessPartnerCode(String businessPartnerCode);

    @Query("""
            SELECT COUNT(bp) FROM BusinessPartner bp
            WHERE bp.useYn = true
              AND bp.validFrom <= :date
              AND bp.validTo >= :date
            """)
    long countActiveAt(LocalDate date);
}