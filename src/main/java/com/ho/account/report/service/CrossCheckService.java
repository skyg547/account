package com.ho.account.report.service;

import com.ho.account.report.domain.ReportSnapshotDetail;
import com.ho.account.report.domain.ReportSnapshotHeader;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CrossCheckService {

    /**
     * 보고서 스냅샷의 논리적 무결성을 검증합니다.
     * 예: 자산 = 부채 + 자본
     */
    public CrossCheckResult verifyBalanceSheetEquality(ReportSnapshotHeader header) {
        if (!"BS".equals(header.getReportType())) {
            return new CrossCheckResult(true, "Not a Balance Sheet snapshot");
        }

        Map<String, BigDecimal> snapshotMap = header.getDetails().stream()
                .collect(Collectors.toMap(ReportSnapshotDetail::getLineCode, ReportSnapshotDetail::getAmount));

        BigDecimal totalAssets = snapshotMap.getOrDefault("TOTAL_ASSETS", BigDecimal.ZERO);
        BigDecimal totalLiabilities = snapshotMap.getOrDefault("TOTAL_LIABILITIES", BigDecimal.ZERO);
        BigDecimal totalEquity = snapshotMap.getOrDefault("TOTAL_EQUITY", BigDecimal.ZERO);

        BigDecimal difference = totalAssets.subtract(totalLiabilities).subtract(totalEquity);
        boolean isValid = difference.compareTo(BigDecimal.ZERO) == 0;

        return new CrossCheckResult(isValid,
                isValid ? "Balance Sheet is balanced" : "Balance Sheet mismatch: Diff = " + difference);
    }

    public static class CrossCheckResult {
        private final boolean valid;
        private final String message;

        public CrossCheckResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
