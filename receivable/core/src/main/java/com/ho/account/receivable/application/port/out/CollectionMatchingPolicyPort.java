package com.ho.account.receivable.application.port.out;

import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.Receivable;
import java.util.List;
import java.util.Optional;

/**
 * 입금 정보와 미결 채권 후보를 비교하여 자동 매칭 대상을 선택하는 정책 포트입니다.
 */
public interface CollectionMatchingPolicyPort {

    Optional<Receivable> selectMatch(Collection collection, List<Receivable> candidates);
}
