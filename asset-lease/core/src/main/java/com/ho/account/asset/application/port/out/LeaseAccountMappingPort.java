package com.ho.account.asset.application.port.out;

import com.ho.account.asset.domain.LeaseContract;
import java.util.List;

/**
 * 리스 회계 계정 매핑 포트.
 *
 * <p>초보자용 설명: 리스부채, 이자비용, 미지급금 계정은 회사 회계 정책마다 달라질 수 있습니다.
 * 서비스에 계정 코드를 박아두지 않고 이 포트를 통해 설정/정책 어댑터에서 가져옵니다.</p>
 */
public interface LeaseAccountMappingPort {

    LeasePaymentAccounts resolvePaymentAccounts(LeaseContract contract);

    record LeasePaymentAccounts(
            String leaseLiabilityAccountCode,
            String leaseInterestExpenseAccountCode,
            String accountsPayableAccountCode) {

        public LeasePaymentAccounts {
            requireText(leaseLiabilityAccountCode, "leaseLiabilityAccountCode is required");
            requireText(leaseInterestExpenseAccountCode, "leaseInterestExpenseAccountCode is required");
            requireText(accountsPayableAccountCode, "accountsPayableAccountCode is required");
        }

        public List<String> requiredAccountCodes() {
            return List.of(leaseLiabilityAccountCode, leaseInterestExpenseAccountCode, accountsPayableAccountCode);
        }

        private static void requireText(String value, String message) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(message);
            }
        }
    }
}
