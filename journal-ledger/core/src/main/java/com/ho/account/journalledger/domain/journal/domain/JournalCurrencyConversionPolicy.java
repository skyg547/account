package com.ho.account.journalledger.domain.journal.domain;

import com.ho.account.journalledger.domain.ledger.domain.AccountingPrecision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 거래통화 금액을 원장의 고정 기준통화(KRW) 금액으로 환산하는 전표 단위 정책입니다.
 */
public final class JournalCurrencyConversionPolicy {

    public static final String BASE_CURRENCY_CODE = "KRW";
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    private JournalCurrencyConversionPolicy() {
    }

    /**
     * 자동분개처럼 기준통화 금액을 생성하는 경로에서 사용합니다.
     *
     * <p>각 차대 측의 거래통화 합계를 한 번 HALF_UP한 금액을 목표로 삼고, 각 라인은 먼저
     * DOWN한 뒤 큰 소수 잔여부터 원장 최소 단위(0.01)씩 배분합니다. 라인별 독립 반올림으로 생기는
     * 차대 불일치를 피하기 위한 전표 단위 배분입니다.</p>
     */
    public static void calculateAndAssignBaseAmounts(
            String currencyCode,
            BigDecimal exchangeRate,
            List<JournalDetail> details) {
        BigDecimal normalizedRate = normalizeExchangeRate(currencyCode, exchangeRate);
        requireDetails(details);

        assignSide(details, JournalSide.DEBIT, normalizedRate);
        assignSide(details, JournalSide.CREDIT, normalizedRate);
    }

    /**
     * 수동·계약·재로딩 전표가 같은 환산 및 잔여 배분 정책을 따르는지 검증합니다.
     */
    public static void validateBaseAmounts(
            String currencyCode,
            BigDecimal exchangeRate,
            List<JournalDetail> details) {
        BigDecimal normalizedRate = normalizeExchangeRate(currencyCode, exchangeRate);
        requireDetails(details);

        validateSide(details, JournalSide.DEBIT, normalizedRate);
        validateSide(details, JournalSide.CREDIT, normalizedRate);
    }

    /**
     * KRW의 생략 환율은 계산상 1로 정규화합니다. 외화는 명시적인 양수 환율이 필수입니다.
     */
    public static BigDecimal normalizeExchangeRate(String currencyCode, BigDecimal exchangeRate) {
        String normalizedCurrency = normalizeCurrencyCode(currencyCode);
        if (BASE_CURRENCY_CODE.equals(normalizedCurrency)) {
            BigDecimal normalizedRate = exchangeRate == null
                    ? AccountingPrecision.exchangeRate(BigDecimal.ONE)
                    : AccountingPrecision.exchangeRate(exchangeRate);
            if (normalizedRate.compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("기준통화 KRW의 환율은 1이어야 합니다.");
            }
            return normalizedRate;
        }

        if (exchangeRate == null) {
            throw new IllegalArgumentException(
                    "외화 전표에는 거래통화-기준통화 환율이 필수입니다: " + normalizedCurrency);
        }
        return AccountingPrecision.exchangeRate(exchangeRate);
    }

    private static String normalizeCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return BASE_CURRENCY_CODE;
        }
        return currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    private static void requireDetails(List<JournalDetail> details) {
        Objects.requireNonNull(details, "전표 상세 목록은 필수입니다.");
        if (details.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("전표 상세는 필수입니다.");
        }
    }

    private static void assignSide(
            List<JournalDetail> details,
            JournalSide side,
            BigDecimal exchangeRate) {
        ConversionPlan plan = buildPlan(details, side, exchangeRate);
        List<LineConversion> allocationOrder = new ArrayList<>(plan.lines());
        // 안정 정렬로 같은 잔여는 생성 시 encounter order에 따라 배분합니다.
        allocationOrder.sort(Comparator.comparing(LineConversion::fractionalRemainder).reversed());

        for (int index = 0; index < allocationOrder.size(); index++) {
            LineConversion line = allocationOrder.get(index);
            BigDecimal baseAmount = index < plan.residualUnits()
                    ? line.floorAmount().add(ONE_CENT)
                    : line.floorAmount();
            line.detail().setBaseAmount(AccountingPrecision.positiveLedgerAmount(baseAmount));
        }
    }

    private static void validateSide(
            List<JournalDetail> details,
            JournalSide side,
            BigDecimal exchangeRate) {
        ConversionPlan plan = buildPlan(details, side, exchangeRate);
        List<LineConversion> remainderOrder = new ArrayList<>(plan.lines());
        remainderOrder.sort(Comparator.comparing(LineConversion::fractionalRemainder).reversed());

        int remainingAdjustments = plan.residualUnits();
        int groupStart = 0;
        while (groupStart < remainderOrder.size()) {
            int groupEnd = groupStart + 1;
            BigDecimal remainder = remainderOrder.get(groupStart).fractionalRemainder();
            while (groupEnd < remainderOrder.size()
                    && remainderOrder.get(groupEnd).fractionalRemainder().compareTo(remainder) == 0) {
                groupEnd++;
            }

            int groupSize = groupEnd - groupStart;
            int requiredAdjusted = Math.min(remainingAdjustments, groupSize);
            int actualAdjusted = 0;
            for (int index = groupStart; index < groupEnd; index++) {
                LineConversion line = remainderOrder.get(index);
                BigDecimal supplied = AccountingPrecision.positiveLedgerAmount(line.detail().getBaseAmount());
                BigDecimal adjusted = line.floorAmount().add(ONE_CENT);
                if (supplied.compareTo(adjusted) == 0) {
                    actualAdjusted++;
                } else if (supplied.compareTo(line.floorAmount()) != 0) {
                    throw inconsistentBaseAmount(line, exchangeRate);
                }
            }

            // JPA 컬렉션 순서가 바뀌어도 동률 그룹 안에서 필요한 조정 라인 수만 같으면 유효합니다.
            if (actualAdjusted != requiredAdjusted) {
                throw new IllegalStateException(
                        "기준통화 환산 잔여가 통제된 라인 수와 일치하지 않습니다. ("
                                + side + ": required=" + requiredAdjusted
                                + ", actual=" + actualAdjusted + ")");
            }
            remainingAdjustments -= requiredAdjusted;
            groupStart = groupEnd;
        }

        if (remainingAdjustments != 0) {
            throw new IllegalStateException("기준통화 환산 잔여를 모두 배분할 수 없습니다: " + side);
        }
    }

    private static ConversionPlan buildPlan(
            List<JournalDetail> details,
            JournalSide side,
            BigDecimal exchangeRate) {
        List<LineConversion> lines = new ArrayList<>();
        BigDecimal transactionTotal = BigDecimal.ZERO;
        BigDecimal floorTotal = BigDecimal.ZERO;

        for (JournalDetail detail : details) {
            if (detail.getSide() == null) {
                throw new IllegalStateException("전표 라인의 차대 구분은 필수입니다.");
            }
            if (detail.getSide() != side) {
                continue;
            }

            BigDecimal amount = AccountingPrecision.positiveLedgerAmount(detail.getAmount());
            BigDecimal exactBaseAmount = amount.multiply(exchangeRate);
            BigDecimal floorAmount = exactBaseAmount.setScale(
                    AccountingPrecision.LEDGER_SCALE,
                    RoundingMode.DOWN);
            BigDecimal fractionalRemainder = exactBaseAmount.subtract(floorAmount);
            lines.add(new LineConversion(detail, floorAmount, fractionalRemainder));
            transactionTotal = transactionTotal.add(amount);
            floorTotal = floorTotal.add(floorAmount);
        }

        BigDecimal targetTotal = transactionTotal.multiply(exchangeRate).setScale(
                AccountingPrecision.LEDGER_SCALE,
                RoundingMode.HALF_UP);
        int residualUnits = residualUnits(targetTotal, floorTotal, lines.size(), side);
        return new ConversionPlan(List.copyOf(lines), residualUnits);
    }

    private static int residualUnits(
            BigDecimal targetTotal,
            BigDecimal floorTotal,
            int lineCount,
            JournalSide side) {
        final int units;
        try {
            units = targetTotal.subtract(floorTotal).movePointRight(
                    AccountingPrecision.LEDGER_SCALE).intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalStateException("기준통화 환산 잔여가 원장 최소 단위가 아닙니다: " + side, exception);
        }
        if (units < 0 || units > lineCount) {
            throw new IllegalStateException(
                    "기준통화 환산 잔여를 통제된 범위에서 배분할 수 없습니다. ("
                            + side + ": residualUnits=" + units + ", lines=" + lineCount + ")");
        }
        return units;
    }

    private static IllegalStateException inconsistentBaseAmount(
            LineConversion line,
            BigDecimal exchangeRate) {
        return new IllegalStateException(
                "기준통화 금액이 거래금액과 환율에 따른 환산 정책과 일치하지 않습니다. "
                        + "(amount=" + line.detail().getAmount()
                        + ", exchangeRate=" + exchangeRate
                        + ", baseAmount=" + line.detail().getBaseAmount() + ")");
    }

    private record LineConversion(
            JournalDetail detail,
            BigDecimal floorAmount,
            BigDecimal fractionalRemainder) {
    }

    private record ConversionPlan(List<LineConversion> lines, int residualUnits) {
    }
}
