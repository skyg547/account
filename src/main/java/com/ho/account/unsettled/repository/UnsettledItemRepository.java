package com.ho.account.unsettled.repository;

import com.ho.account.unsettled.domain.UnsettledItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnsettledItemRepository extends JpaRepository<UnsettledItem, Long> {
    // 특정 거래처의 미결 항목 조회 (잔액이 있는 것만)
    List<UnsettledItem> findByBusinessPartnerBusinessPartnerCodeAndStatusNot(String businessPartnerCode, String status);

    // 특정 계정의 미결 항목 조회
    List<UnsettledItem> findByAccountSubjectCodeAndStatusNot(String accountCode, String status);
}
