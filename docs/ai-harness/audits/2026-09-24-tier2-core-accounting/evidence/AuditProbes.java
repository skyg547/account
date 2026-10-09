import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.common.adapter.FiscalPeriodAccountingPeriodStatusAdapter;
import com.ho.account.contracts.masterdata.*;
import com.ho.account.journalledger.domain.journal.domain.*;
import com.ho.account.journalledger.domain.ledger.domain.*;
import com.ho.account.journalledger.application.port.out.*;
import com.ho.account.journalledger.application.service.journal.*;
import com.ho.account.journalledger.application.service.journal.validator.*;
import com.ho.account.journalledger.application.service.ledger.*;
import com.ho.account.closing.application.service.*;
import com.ho.account.closing.application.port.out.*;
import com.ho.account.closing.domain.*;
import com.ho.account.closing.batch.adapter.out.JournalFxValuationBalanceSource;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Synthetic source-execution probes; PASS means the stated observation was reproduced, not that a defect is fixed. */
public class AuditProbes {
    static final LocalDate DAY = LocalDate.of(2026, 9, 1);
    static final List<Map<String, Object>> results = new ArrayList<>();
    interface Probe { String run() throws Exception; }
    static BigDecimal b(String s) { return new BigDecimal(s); }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void rejected(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException ex) { return; }
        throw new AssertionError("Expected rejection");
    }
    static void run(String id, String type, Probe probe) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("id", id); result.put("kind", type);
        try { result.put("observation", probe.run()); result.put("status", "PASS"); }
        catch (Throwable ex) { result.put("status", "FAIL"); result.put("error", ex.toString()); }
        results.add(result);
        System.out.println(id + " " + result.get("status") + " " + result.getOrDefault("observation", result.get("error")));
    }
    static JournalDetail line(JournalSide side, String account, String amount, String base) {
        JournalDetail d = new JournalDetail(); d.setSide(side); d.setAccountCode(account);
        d.setAmount(b(amount)); d.setBaseAmount(b(base)); return d;
    }
    static JournalEntry journal() {
        JournalEntry e = new JournalEntry(); e.setId(1L); e.setSlipNo("AUDIT-1");
        e.setSlipDate(DAY); e.setAccountingDate(DAY); e.setCurrencyCode("KRW");
        e.setCreatedBy("maker"); e.initializeDraft();
        e.addDetail(line(JournalSide.DEBIT,"CASH","100","100"));
        e.addDetail(line(JournalSide.CREDIT,"REVENUE","100","100"));
        e.getDetails().get(0).setId(10L); e.getDetails().get(1).setId(11L); return e;
    }
    static JournalDetail dated(LocalDate date, String amount, String partner) {
        JournalEntry e = journal(); e.setAccountingDate(date);
        JournalDetail d = e.getDetails().get(0); d.setAmount(b(amount)); d.setBaseAmount(b(amount));
        d.setBusinessPartnerCode(partner); return d;
    }
    // Stateful port double preserves the actual service's read/write order; it is not a database/concurrency test.
    static class Balances {
        final LedgerBalancePersistencePort port = mock(LedgerBalancePersistencePort.class);
        final Map<LocalDate, GlBalance> gl = new TreeMap<>();
        final List<SlBalance> sl = new ArrayList<>();
        final LedgerService service;
        Balances() {
            when(port.findGlBalance(anyString(),anyString(),any(),any())).thenAnswer(i -> Optional.ofNullable(gl.get(i.getArgument(2))));
            when(port.findPreviousGlBalance(anyString(),anyString(),any())).thenAnswer(i -> gl.entrySet().stream()
                .filter(e -> e.getKey().isBefore(i.getArgument(2))).max(Map.Entry.comparingByKey()).map(Map.Entry::getValue));
            doAnswer(i -> { List<GlBalance> values=i.getArgument(0); values.forEach(v -> gl.put(v.getBalanceDate(),v)); return null; }).when(port).saveGlBalances(anyList());
            doAnswer(i -> { sl.addAll(i.getArgument(0)); return null; }).when(port).saveSlBalances(anyList());
            service = new LedgerService(port);
        }
    }
    static ClosingJournalEntryPort capture(List<ClosingJournalEntryCommand> commands) {
        ClosingJournalEntryPort p = mock(ClosingJournalEntryPort.class);
        when(p.createDraftAdjustment(any())).thenAnswer(i -> { ClosingJournalEntryCommand c=i.getArgument(0); commands.add(c); return new ClosingJournalEntryResult((long)commands.size(),c.slipNo()); });
        return p;
    }
    static MasterDataQueryPort accounts(boolean fixedAsset, String category) {
        MasterDataQueryPort p = mock(MasterDataQueryPort.class);
        when(p.findAccountSubjectAt(anyString(),any())).thenAnswer(i -> Optional.of(new AccountSubjectRef(i.getArgument(0),"Synthetic",false,fixedAsset,"DEBIT",category)));
        return p;
    }
    public static void main(String[] args) throws Exception {
        run("P01", "positive-control", () -> {
            JournalEntry e=journal(); e.clearDetails(); rejected(e::validateBalance);
            e.addDetail(line(JournalSide.DEBIT,"CASH","100","100")); rejected(e::validateBalance);
            e=journal(); e.getDetails().get(1).setAmount(b("99.99")); rejected(e::validateBalance);
            e=journal(); e.getDetails().get(1).setBaseAmount(b("99.99")); rejected(e::validateBalance);
            for(String value: List.of("0","-1","0.001","100000000000000000.00")) rejected(() -> line(JournalSide.DEBIT,"CASH",value,"1"));
            AccountingPrecision.positiveLedgerAmount(b("1.000")); journal().validateBalance();
            return "empty, single, transaction/base imbalance, zero, negative, fractional cent and overflow rejected; balanced and lossless trailing-zero values accepted";
        });
        run("P02", "defect-reproduction", () -> {
            JournalEntry e=journal(); e.approve("maker"); check(e.getStatus()==JournalEntryStatus.APPROVED,"self approval");
            return "creator maker approved own DRAFT directly without submission";
        });
        run("P03", "defect-reproduction", () -> {
            JournalEntry e=journal(); e.approve("checker"); e.post("poster");
            e.setAccountingDate(DAY.plusMonths(1)); e.getDetails().get(0).setAmount(b("999")); e.clearDetails();
            check(e.getStatus()==JournalEntryStatus.POSTED && e.getDetails().isEmpty(),"posted mutation");
            return "POSTED accepted date change, line amount change, and clearDetails in memory; no HTTP/delete or DB-cascade success asserted";
        });
        run("P04", "defect-reproduction", () -> {
            JournalEntry e=journal(); e.approve("checker"); e.post("poster");
            JournalEntry r1=e.createReversal("maker",DAY.plusDays(1),"cancel");
            JournalEntry r2=e.createReversal("maker",DAY.plusDays(1),"cancel retry");
            check(e.getStatus()==JournalEntryStatus.POSTED && r1!=r2,"duplicate reversal");
            check(r1.getDetails().get(0).getSide()==JournalSide.CREDIT,"real inverted line");
            return "two independent inverted reversal objects created; original remains POSTED; persistence/date validation occurs in service";
        });
        run("P05", "defect-reproduction", () -> {
            JournalEntry e=journal(); e.setCurrencyCode("USD"); e.setExchangeRate(b("1300"));
            e.validateBalance(); e.approve("checker"); GeneralLedger.fromApproved(e);
            return "USD100 at rate1300 accepted with base100 on each side, instead of base130000";
        });
        run("P06", "defect-reproduction", () -> {
            Map<?,?> m=new ObjectMapper().readValue("{\"amount\":900719925474099.11}",Map.class);
            BigDecimal observed=AccountingPrecision.positiveLedgerAmount(new BigDecimal(m.get("amount").toString()));
            check(observed.compareTo(b("900719925474099.11"))!=0,"precision loss");
            return "untyped JSON 900719925474099.11 becomes " + observed + " and passes ledger precision";
        });
        run("P07", "defect-reproduction", () -> {
            FiscalPeriodControlPort p=mock(FiscalPeriodControlPort.class);
            when(p.findFiscalPeriod(anyString(),anyString())).thenReturn(Optional.of(new FiscalPeriodRef(1L,"2026","09",DAY,DAY.plusMonths(1).minusDays(1),"CLOSING_IN_PROGRESS")));
            boolean closed=new FiscalPeriodAccountingPeriodStatusAdapter(p).isClosed(DAY);
            check(!closed,"unknown period status accepted"); return "CLOSING_IN_PROGRESS returned isClosed=false";
        });
        run("P08", "defect-reproduction", () -> {
            Balances s=new Balances(); s.service.updateLedgerBalancesBulk(List.of(dated(DAY.plusDays(1),"20",null),dated(DAY,"100",null)));
            check(s.gl.get(DAY.plusDays(1)).getEndingBalance().compareTo(b("20"))==0,"unsorted sum");
            return "out-of-order bulk input: day2 ending20, expected120";
        });
        run("P09", "defect-reproduction", () -> {
            Balances s=new Balances(); s.service.updateLedgerBalancesBulk(List.of(dated(DAY,"100",null),dated(DAY.plusDays(1),"20",null)));
            s.service.updateLedgerBalancesBulk(List.of(dated(DAY,"10",null)));
            check(s.gl.get(DAY.plusDays(1)).getEndingBalance().compareTo(b("120"))==0,"stale carry");
            return "backdated +10 changes day1 to110 but day2 remains120, expected130";
        });
        run("P10", "defect-reproduction", () -> {
            Balances s=new Balances(); s.service.updateLedgerBalancesBulk(List.of(dated(DAY,"10",null),dated(DAY,"20","NULL")));
            check(s.sl.size()==1 && s.sl.get(0).getDebitAmount().compareTo(b("30"))==0,"SL collision");
            return "partner null and literal NULL collapsed into one SL row30";
        });
        run("P11", "defect-reproduction", () -> {
            JournalPersistencePort p=mock(JournalPersistencePort.class); when(p.save(any())).thenAnswer(i->i.getArgument(0));
            JournalEntryService s=new JournalEntryService(p,null,null,new JournalValidationEngine(List.of(new BalanceValidationFilter())));
            Set<String> slips=new HashSet<>(); int duplicates=0;
            for(int n=0;n<5000;n++){ JournalEntry e=journal(); e.setSlipNo(null); if(!slips.add(s.createJournalEntry(e).getSlipNo())) duplicates++; }
            check(duplicates>0,"bounded random collision sample unexpectedly had no collision");
            return "5000 same-day create calls generated "+duplicates+" duplicate slip numbers (observational random sample)";
        });
        run("P12", "defect-reproduction", () -> {
            DriverManagerDataSource ds=new DriverManagerDataSource("jdbc:h2:mem:fxAudit;DB_CLOSE_DELAY=-1","sa","");
            JdbcTemplate jdbc=new JdbcTemplate(ds);
            jdbc.execute("CREATE TABLE journal_entries(id BIGINT,status VARCHAR(20),accounting_date DATE,currency_code VARCHAR(3))");
            jdbc.execute("CREATE TABLE journal_details(journal_entry_id BIGINT,account_code VARCHAR(50),side VARCHAR(10),amount DECIMAL(19,2),base_amount DECIMAL(19,2))");
            jdbc.update("INSERT INTO journal_entries VALUES(1,'POSTED',?,'USD'),(2,'POSTED',?,'KRW')",DAY,DAY);
            jdbc.update("INSERT INTO journal_details VALUES(1,'CASH','DEBIT',100,1000),(2,'CASH','DEBIT',200,200)");
            var reader=new JournalFxValuationBalanceSource(ds,jdbc).createReader(DAY,"KRW","CASH","CASH",100);
            reader.open(new ExecutionContext()); FxValuationBalance balance=reader.read(); reader.close();
            check(balance.bookReportingAmount().compareTo(b("1000"))==0,"prior adjustment excluded");
            List<ClosingJournalEntryCommand> commands=new ArrayList<>();
            FxValuationService s=new FxValuationService((f,t,d)->Optional.of(b("12")),capture(commands),new ClosingAccountingProperties(),accounts(false,"ASSET"));
            s.processFxValuationForAccount(balance,DAY,2L);
            check(commands.get(0).lines().get(0).amount().compareTo(b("200"))==0,"repeat gain");
            return "actual SQL ignores posted KRW200 prior adjustment; service generates another200 at unchanged rate12";
        });
        run("P13", "defect-reproduction", () -> {
            List<ClosingJournalEntryCommand> commands=new ArrayList<>();
            FxValuationService s=new FxValuationService((f,t,d)->Optional.of(b("12")),capture(commands),new ClosingAccountingProperties(),accounts(true,"ASSET"));
            s.processFxValuationForAccount(new FxValuationBalance("PPE","USD",b("100"),b("1000")),DAY,1L);
            check(commands.size()==1,"fixed asset revalued"); return "fixedAsset=true historical-cost candidate revalued by200 with no eligibility check";
        });
        run("P14", "defect-reproduction", () -> {
            ClosingAccountingProperties props=new ClosingAccountingProperties();
            var rule=new ClosingAccountingProperties.AutomatedJournalRule(); rule.setDebitAccountCode("BAD_DEBT"); rule.setCreditAccountCode("ALLOWANCE"); rule.setAmount(BigDecimal.ONE);
            props.setProvisionRules(Map.of(ProvisionBatch.ProvisionType.ECL,rule));
            List<ClosingJournalEntryCommand> commands=new ArrayList<>();
            EclAllowanceSummary summary=new EclAllowanceSummary(DAY,"run1","model1","ENTITY","USD","LOAN","ALLOWANCE","BAD_DEBT","RELEASE",b("100"),b("1000"),b("100"),BigDecimal.ZERO,BigDecimal.ZERO);
            EclProvisionService s=new EclProvisionService((a,c,d)->b("104000"),capture(commands),props,d->List.of(summary));
            s.processEclProvision(DAY,1L);
            var first=commands.get(0).lines().get(0);
            check(first.side()==ClosingJournalSide.DEBIT && first.amount().compareTo(b("103900"))==0,"ECL unit mismatch");
            return "USD target100 minus stored base credit104000 generates allowance DEBIT103900; expected USD20 addition at rate1300";
        });
        run("P15", "defect-reproduction", () -> {
            var summary=new EclAllowanceSummary(DAY,"run1","model1","ENTITY","KRW","LOAN","ALLOWANCE","EXPENSE","RELEASE",b("100"),b("1000"),b("1"),b("2"),b("3"));
            check(summary.targetAllowanceAmount().compareTo(b("100"))==0,"stage mismatch accepted");
            return "target allowance100 accepted with stage totals1+2+3=6; constructor checks only nonnegative values";
        });
        run("P16", "defect-reproduction", () -> {
            var task=new ClosingTask(); task.setMandatory(true); task.setStatus(ClosingTask.ClosingTaskStatus.PENDING); task.start("maker"); task.complete("maker");
            var gate=new ClosingGate(); gate.setStatus(ClosingGate.ClosingGateStatus.PENDING); gate.pass("checker",List.of(task));
            var calendar=new ClosingCalendar(); calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.OPEN); calendar.start("maker");
            calendar.validateReadyToClose(List.of(task),List.of(gate)); calendar.close("checker"); calendar.reopen("approver"); calendar.start("maker");
            calendar.validateReadyToClose(List.of(task),List.of(gate)); calendar.close("checker");
            check(calendar.getStatus()==ClosingCalendar.ClosingCalendarStatus.CLOSED,"stale evidence reuse");
            return "close/reopen/start/reclose accepted original completed task and passed gate";
        });
        run("P17", "defect-reproduction", () -> {
            var q=mock(com.ho.account.contracts.journal.JournalQueryPort.class);
            var p=mock(com.ho.account.contracts.journal.JournalPostingPort.class);
            var summaries=new ArrayList<com.ho.account.contracts.journal.JournalSummary>();
            var posted=new com.ho.account.contracts.journal.JournalSummary(); posted.setId(1L); posted.setStatus("POSTED"); summaries.add(posted);
            var detail=new com.ho.account.contracts.journal.JournalDetailSummary(); detail.setAccountCode("REVENUE"); detail.setAccountCategory("REVENUE");
            detail.setSide(com.ho.account.contracts.journal.JournalSide.CREDIT); detail.setBaseAmount(b("1000"));
            when(q.getJournalSummaries(any(),any())).thenReturn(summaries); when(q.getJournalDetails(anyLong())).thenReturn(List.of(detail));
            var commands=new ArrayList<com.ho.account.contracts.journal.JournalEntryCommand>();
            when(p.createDraftEntry(any())).thenAnswer(i -> { commands.add(i.getArgument(0)); return null; });
            var service=new AnnualClosingService(q,p); service.performIncomeStatementClosing(2026,"CASH_ASSET");
            var command=commands.get(0); check(command.lines().get(1).accountCode().equals("CASH_ASSET"),"arbitrary equity account");
            var existing=new com.ho.account.contracts.journal.JournalSummary(); existing.setId(2L); existing.setStatus("DRAFT"); existing.setSlipNo(command.slipNo());
            existing.setAccountingDate(LocalDate.of(2026,12,31)); existing.setDescription(command.description()); existing.setEntryType("TRANSFER"); summaries.add(existing);
            detail.setBaseAmount(b("1500")); service.performIncomeStatementClosing(2026,"CASH_ASSET");
            check(commands.size()==1 && commands.get(0).lines().get(1).amount().compareTo(b("1000"))==0,"stale annual draft");
            return "annual close accepted CASH_ASSET as retained earnings; source1000->1500 left original1000 draft unchanged";
        });
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new java.io.File("/tmp/account-tier2-audit-evidence/probe-results.json"),results);
        if(results.stream().anyMatch(r->!"PASS".equals(r.get("status")))) System.exit(1);
    }
}
