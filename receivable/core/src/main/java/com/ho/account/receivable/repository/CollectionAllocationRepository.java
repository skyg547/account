package com.ho.account.receivable.repository;

import com.ho.account.receivable.infrastructure.persistence.entity.CollectionAllocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionAllocationRepository extends JpaRepository<CollectionAllocationJpaEntity, Long> {
}
