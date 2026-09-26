package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P09 audited-source proof: the test uses the pre-fix public service call and a stateful port.
 * Reflection keeps the same source usable when LedgerService has either its audited one-port
 * constructor or the later V15 control-port constructor.
 */
class BackdatedBalanceAuditedRegressionTest {

    @Test
    void lateDayOneDebitRepairsTheAlreadyMaterializedDayTwoGlAndSlBalances() throws Exception {
        LocalDate dayOne = LocalDate.of(2026, 9, 1);
        LocalDate dayTwo = dayOne.plusDays(1);
        StatefulBalances state = new StatefulBalances();
        LedgerService service = ledgerService(state.port());

        service.updateLedgerBalances(detail(dayOne, "100.00"), dayOne);
        service.updateLedgerBalances(detail(dayTwo, "20.00"), dayTwo);
        service.updateLedgerBalances(detail(dayOne, "10.00"), dayOne);

        GlBalance gl = state.gl.get(new GlKey("10100", "KRW", dayTwo));
        SlBalance sl = state.sl.get(new SlKey("10100", "BP-P09", "D-P09", "KRW", dayTwo));
        assertThat(gl.getBeginningBalance()).isEqualByComparingTo("110.00");
        assertThat(gl.getDebitAmount()).isEqualByComparingTo("20.00");
        assertThat(gl.getCreditAmount()).isEqualByComparingTo("0.00");
        assertThat(gl.getEndingBalance()).isEqualByComparingTo("130.00");
        assertThat(sl.getBeginningBalance()).isEqualByComparingTo("110.00");
        assertThat(sl.getDebitAmount()).isEqualByComparingTo("20.00");
        assertThat(sl.getCreditAmount()).isEqualByComparingTo("0.00");
        assertThat(sl.getEndingBalance()).isEqualByComparingTo("130.00");
    }

    private LedgerService ledgerService(LedgerBalancePersistencePort balances) throws Exception {
        for (Constructor<?> constructor : LedgerService.class.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 0 || !parameterTypes[0].isInstance(balances)) continue;
            Object[] arguments = new Object[parameterTypes.length];
            arguments[0] = balances;
            for (int index = 1; index < parameterTypes.length; index++) {
                Class<?> contract = parameterTypes[index];
                arguments[index] = Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[]{contract},
                        (proxy, method, args) -> defaultValue(method.getReturnType()));
            }
            constructor.setAccessible(true);
            return (LedgerService) constructor.newInstance(arguments);
        }
        throw new IllegalStateException("LedgerService constructor does not accept the balance port");
    }

    private JournalDetail detail(LocalDate date, String amount) {
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(date);
        entry.setCurrencyCode("KRW");
        JournalDetail detail = new JournalDetail();
        detail.setJournalEntry(entry);
        detail.setSide(JournalSide.DEBIT);
        detail.setAccountCode("10100");
        detail.setBusinessPartnerCode("BP-P09");
        detail.setDepartmentCode("D-P09");
        detail.setBaseAmount(new BigDecimal(amount));
        return detail;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }

    private static final class StatefulBalances implements InvocationHandler {
        private final Map<GlKey, GlBalance> gl = new LinkedHashMap<>();
        private final Map<SlKey, SlBalance> sl = new LinkedHashMap<>();

        LedgerBalancePersistencePort port() {
            return (LedgerBalancePersistencePort) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{LedgerBalancePersistencePort.class}, this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            return switch (method.getName()) {
                case "findGlBalance" -> Optional.ofNullable(gl.get(new GlKey(
                        (String) args[0], (String) args[1], (LocalDate) args[2])));
                case "findPreviousGlBalance" -> previousGl((String) args[0], (String) args[1], (LocalDate) args[2]);
                case "saveGlBalance" -> saveGl((GlBalance) args[0]);
                case "saveGlBalances" -> { ((List<GlBalance>) args[0]).forEach(this::saveGl); yield null; }
                case "findSlBalance" -> Optional.ofNullable(sl.get(new SlKey(
                        (String) args[0], (String) args[1], (String) args[2], (String) args[3], (LocalDate) args[4])));
                case "findPreviousSlBalance" -> previousSl((String) args[0], (String) args[1],
                        (String) args[2], (String) args[3], (LocalDate) args[4]);
                case "saveSlBalance" -> saveSl((SlBalance) args[0]);
                case "saveSlBalances" -> { ((List<SlBalance>) args[0]).forEach(this::saveSl); yield null; }
                case "shiftSuccessorBalances" -> { shiftSuccessors((LocalDate) args[0],
                        (List<?>) args[1], (List<?>) args[2]); yield null; }
                case "toString" -> "stateful audited balance port";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            };
        }

        private Optional<GlBalance> previousGl(String account, String currency, LocalDate before) {
            return gl.entrySet().stream().filter(entry -> entry.getKey().account().equals(account)
                            && entry.getKey().currency().equals(currency) && entry.getKey().date().isBefore(before))
                    .max(Map.Entry.comparingByKey(Comparator.comparing(GlKey::date))).map(Map.Entry::getValue);
        }

        private Optional<SlBalance> previousSl(String account, String partner, String department,
                                               String currency, LocalDate before) {
            return sl.entrySet().stream().filter(entry -> entry.getKey().sameDimension(
                            account, partner, department, currency) && entry.getKey().date().isBefore(before))
                    .max(Map.Entry.comparingByKey(Comparator.comparing(SlKey::date))).map(Map.Entry::getValue);
        }

        private GlBalance saveGl(GlBalance balance) {
            gl.put(new GlKey(balance.getAccountCode(), balance.getCurrencyCode(), balance.getBalanceDate()), balance);
            return balance;
        }

        private SlBalance saveSl(SlBalance balance) {
            sl.put(new SlKey(balance.getAccountCode(), balance.getBusinessPartnerCode(),
                    balance.getDepartmentCode(), balance.getCurrencyCode(), balance.getBalanceDate()), balance);
            return balance;
        }

        private void shiftSuccessors(LocalDate postingDate, List<?> glDeltas, List<?> slDeltas) throws Exception {
            for (Object delta : glDeltas) {
                String account = accessor(delta, "accountCode", String.class);
                String currency = accessor(delta, "currencyCode", String.class);
                BigDecimal amount = accessor(delta, "signedDelta", BigDecimal.class);
                gl.forEach((key, balance) -> {
                    if (key.account().equals(account) && key.currency().equals(currency)
                            && key.date().isAfter(postingDate)) shift(balance, amount);
                });
            }
            for (Object delta : slDeltas) {
                String account = accessor(delta, "accountCode", String.class);
                String partner = accessor(delta, "businessPartnerCode", String.class);
                String department = accessor(delta, "departmentCode", String.class);
                String currency = accessor(delta, "currencyCode", String.class);
                BigDecimal amount = accessor(delta, "signedDelta", BigDecimal.class);
                sl.forEach((key, balance) -> {
                    if (key.sameDimension(account, partner, department, currency)
                            && key.date().isAfter(postingDate)) shift(balance, amount);
                });
            }
        }

        private <T> T accessor(Object target, String name, Class<T> type) throws Exception {
            return type.cast(target.getClass().getMethod(name).invoke(target));
        }

        private void shift(GlBalance balance, BigDecimal amount) {
            balance.setBeginningBalance(balance.getBeginningBalance().add(amount));
            balance.setEndingBalance(balance.getEndingBalance().add(amount));
        }

        private void shift(SlBalance balance, BigDecimal amount) {
            balance.setBeginningBalance(balance.getBeginningBalance().add(amount));
            balance.setEndingBalance(balance.getEndingBalance().add(amount));
        }
    }

    private record GlKey(String account, String currency, LocalDate date) { }
    private record SlKey(String account, String partner, String department, String currency, LocalDate date) {
        boolean sameDimension(String expectedAccount, String expectedPartner,
                              String expectedDepartment, String expectedCurrency) {
            return java.util.Objects.equals(account, expectedAccount)
                    && java.util.Objects.equals(partner, expectedPartner)
                    && java.util.Objects.equals(department, expectedDepartment)
                    && java.util.Objects.equals(currency, expectedCurrency);
        }
    }
}
