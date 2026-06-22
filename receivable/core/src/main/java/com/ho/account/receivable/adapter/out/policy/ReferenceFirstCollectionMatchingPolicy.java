package com.ho.account.receivable.adapter.out.policy;

import com.ho.account.receivable.application.port.out.CollectionMatchingPolicyPort;
import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.Receivable;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 참조번호를 가장 신뢰하고, 그다음 만기일과 금액을 이용하는 기본 자동 매칭 정책입니다.
 *
 * <p>🐣 후보가 둘 이상이면 임의로 하나를 고르지 않습니다. 잘못된 채권을 지우는 것보다
 * 수동 확인 대기 상태로 남기는 편이 회계 감사와 고객 잔액 정합성에 안전합니다.</p>
 */
@Component
public class ReferenceFirstCollectionMatchingPolicy implements CollectionMatchingPolicyPort {

    private static final long DUE_DATE_TOLERANCE_DAYS = 7L;

    @Override
    public Optional<Receivable> selectMatch(Collection collection, List<Receivable> candidates) {
        List<Receivable> eligible = candidates.stream()
                .filter(candidate -> candidate.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0)
                .filter(candidate -> candidate.getOutstandingAmount().compareTo(collection.getUnallocatedAmount()) >= 0)
                .toList();

        if (collection.getReferenceNo() != null && !collection.getReferenceNo().isBlank()) {
            List<Receivable> referenceMatches = eligible.stream()
                    .filter(candidate -> candidate.getSalesInvoice() != null)
                    .filter(candidate -> collection.getReferenceNo().trim()
                            .equalsIgnoreCase(candidate.getSalesInvoice().getInvoiceNo()))
                    .toList();
            if (!referenceMatches.isEmpty()) {
                return unique(referenceMatches);
            }
        }

        Optional<Receivable> dueDateMatch = unique(eligible.stream()
                .filter(candidate -> candidate.getDueDate() != null && collection.getCollectionDate() != null)
                .filter(candidate -> Math.abs(ChronoUnit.DAYS.between(
                        candidate.getDueDate(), collection.getCollectionDate())) <= DUE_DATE_TOLERANCE_DAYS)
                .toList());
        if (dueDateMatch.isPresent()) {
            return dueDateMatch;
        }

        return unique(eligible.stream()
                .filter(candidate -> candidate.getOutstandingAmount()
                        .compareTo(collection.getUnallocatedAmount()) == 0)
                .toList());
    }

    private Optional<Receivable> unique(List<Receivable> candidates) {
        return candidates.size() == 1 ? Optional.of(candidates.get(0)) : Optional.empty();
    }
}
