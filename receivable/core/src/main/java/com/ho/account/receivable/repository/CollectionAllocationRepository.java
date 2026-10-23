package com.ho.account.receivable.repository;

import com.ho.account.receivable.infrastructure.persistence.entity.CollectionAllocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CollectionAllocationRepository extends JpaRepository<CollectionAllocationJpaEntity, Long> {
    Optional<CollectionAllocationJpaEntity> findByCollectionIdAndOperationId(Long collectionId, String operationId);
}
