package com.ho.account.income.repository;

import com.ho.account.income.domain.Collection;
import com.ho.account.income.domain.UnmatchedCollection;
import com.ho.account.income.domain.UnmatchedCollectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface UnmatchedCollectionRepository extends JpaRepository<UnmatchedCollection, Long> {
    List<UnmatchedCollection> findByStatus(UnmatchedCollectionStatus status);
    List<UnmatchedCollection> findByCollection(Collection collection);
}
