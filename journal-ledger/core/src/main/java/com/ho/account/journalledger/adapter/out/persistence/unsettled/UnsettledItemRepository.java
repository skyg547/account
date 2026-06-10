package com.ho.account.journalledger.adapter.out.persistence.unsettled;

import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 미결 항목 JPA 저장소.
 *
 * <p>Spring Data JPA 기술 계약이므로 애플리케이션 서비스가 직접 사용하지 않습니다.
 * {@code UnsettledItemPersistenceAdapter}가 이 저장소를 감싸 출력 포트를 구현합니다.</p>
 */
@Repository
public interface UnsettledItemRepository extends JpaRepository<UnsettledItem, Long> {
    // 관리번호로 미결 항목 조회
    List<UnsettledItem> findByManagementNo(String managementNo);

    // 완전히 반제되지 않은 전체 미결 항목 조회
    List<UnsettledItem> findByResolvedFalse();

    // 완전히 반제되지 않은 특정 거래처의 미결 항목만 DB에서 조회
    List<UnsettledItem> findByResolvedFalseAndBusinessPartnerCode(String businessPartnerCode);
}
