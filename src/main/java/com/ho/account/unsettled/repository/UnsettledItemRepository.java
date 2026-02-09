package com.accounting.system.unsettled.repository;

import com.accounting.system.unsettled.domain.UnsettledItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnsettledItemRepository extends JpaRepository<UnsettledItem, Long> {
    // 특정 거래처의 미결 항목 조회 (잔액이 있는 것만)
    List<UnsettledItem> findByCustomerCustomerCodeAndStatusNot(String customerCode, String status);
    
    // 특정 계정의 미결 항목 조회
    List<UnsettledItem> findByAccountSubjectAccountCodeAndStatusNot(String accountCode, String status);
}
