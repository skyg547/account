package com.ho.account.asset.repository;

import com.ho.account.asset.domain.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {
    List<FixedAsset> findByStatus(String status);
}
