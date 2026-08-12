package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.UnmatchedCollectionStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionJpaEntity;
import com.ho.account.receivable.infrastructure.persistence.entity.UnmatchedCollectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnmatchedCollectionRepository extends JpaRepository<UnmatchedCollectionJpaEntity, Long> {
    List<UnmatchedCollectionJpaEntity> findByStatus(UnmatchedCollectionStatus status);
    List<UnmatchedCollectionJpaEntity> findByCollection(CollectionJpaEntity collection);
}
