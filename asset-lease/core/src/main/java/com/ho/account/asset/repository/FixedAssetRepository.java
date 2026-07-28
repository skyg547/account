package com.ho.account.asset.repository;

import com.ho.account.asset.domain.FixedAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {
    List<FixedAsset> findByStatus(String status);

    // [초보 가이드] Spring Batch RepositoryItemReader는 마지막 인자로 Pageable을 붙여 호출합니다.
    // 대량 감가상각은 이 메서드로 ACTIVE 자산을 페이지 단위로 읽고, 계산은 core pipeline에 위임합니다.
    Page<FixedAsset> findByStatus(String status, Pageable pageable);
}