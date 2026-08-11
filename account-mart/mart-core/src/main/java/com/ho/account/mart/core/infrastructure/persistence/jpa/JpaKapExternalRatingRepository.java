package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.external.kap.KapExternalRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [Infrastructure] KAP 외부 평가정보 데이터 접근 JPA Repository.
 */
public interface JpaKapExternalRatingRepository extends JpaRepository<KapExternalRatingEntity, Long> {
}
