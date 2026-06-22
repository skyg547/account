package com.ho.account.receivable.repository;

import com.ho.account.receivable.domain.CollectionAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionAllocationRepository extends JpaRepository<CollectionAllocation, Long> {
}
