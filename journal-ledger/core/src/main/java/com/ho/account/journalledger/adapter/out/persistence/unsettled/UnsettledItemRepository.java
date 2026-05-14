package com.ho.account.journalledger.adapter.out.persistence.unsettled;

import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 誘멸껐 ??ぉ ?덊룷吏?좊━
 */
@Repository
public interface UnsettledItemRepository extends JpaRepository<UnsettledItem, Long> {
    // 愿由?踰덊샇濡?誘멸껐 ??ぉ 議고쉶
    List<UnsettledItem> findByManagementNo(String managementNo);
    
    // 誘멸껐 ?곹깭????ぉ 議고쉶
    List<UnsettledItem> findByResolvedFalse();
}
