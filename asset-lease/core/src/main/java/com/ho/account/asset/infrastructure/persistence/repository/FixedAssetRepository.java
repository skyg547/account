package com.ho.account.asset.infrastructure.persistence.repository;

import com.ho.account.asset.domain.FixedAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * [헥사고날 아키텍처 - 인프라스트럭처 영속성 계층 (Infrastructure Persistence Repository)]
 * 고정자산(FixedAsset) 엔티티에 대한 Spring Data JPA 영속성 인터페이스입니다.
 * 
 * 💡 [교육적 주석 - DIP & 헥사고날 아키텍처 원칙]
 * 1. 헥사고날 아키텍처의 의존성 방향:
 *    Spring Data JPA 인터페이스는 특정 기술(ORM/Spring Data Framework)에 직접 결합된
 *    인프라스트럭처 세부 구현체입니다. 따라서 Application Core(도메인 및 서비스) 내부가 아닌
 *    infrastructure.persistence.repository 패키지에 위치해야 합니다.
 * 
 * 2. 의존관계 역전 원칙 (DIP - Dependency Inversion Principle):
 *    도메인 및 애플리케이션 서비스는 JpaRepository를 직접 의존하지 않고,
 *    Core에 정의된 아웃바운드 포트(FixedAssetPersistencePort)를 참조합니다.
 *    이 JpaRepository는 인프라 어댑터(FixedAssetPersistenceAdapter) 내부에서만 캡슐화되어 사용됩니다.
 */
@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {
    List<FixedAsset> findByStatus(String status);

    Optional<FixedAsset> findByAssetCode(String assetCode);

    // Spring Batch RepositoryItemReader는 마지막 인자로 Pageable을 붙여 호출합니다.
    Page<FixedAsset> findByStatus(String status, Pageable pageable);
}
