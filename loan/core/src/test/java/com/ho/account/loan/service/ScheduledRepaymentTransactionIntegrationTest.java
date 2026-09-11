package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanAccrualLog;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.infrastructure.persistence.EIRAmortizationScheduleRepository;
import com.ho.account.loan.infrastructure.persistence.JpaLoanAccrualPersistenceAdapter;
import com.ho.account.loan.infrastructure.persistence.JpaLoanRepaymentPersistenceAdapter;
import com.ho.account.loan.infrastructure.persistence.LoanAccrualLogRepository;
import com.ho.account.loan.infrastructure.persistence.LoanEventRepository;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Uses real H2/JPA adapters and committed database reads, without a test rollback transaction. */
@SpringJUnitConfig(ScheduledRepaymentTransactionIntegrationTest.Config.class)
class ScheduledRepaymentTransactionIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2090, 1, 15);
    @Autowired ScheduledRepaymentService service;
    @Autowired LoanJournalPort journal;
    @Autowired LoanReferenceDataPort reference;
    @Autowired JpaLoanRepaymentPersistenceAdapter repayment;
    @Autowired LoanRepository loans;
    @Autowired EIRAmortizationScheduleRepository schedules;
    @Autowired LoanAccrualLogRepository accruals;
    @Autowired LoanEventRepository events;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired DataSource dataSource;
    private JdbcTemplate jdbc;
    private Long loanId;

    @BeforeEach
    void seedCommittedLoanAndAccrual() {
        reset(journal, reference, repayment);
        jdbc = new JdbcTemplate(dataSource);
        when(reference.requireAccount(any(), any())).thenAnswer(invocation ->
                new LoanReferenceDataPort.AccountReference(invocation.getArgument(0), "Account"));
        loanId = new TransactionTemplate(transactionManager).execute(status -> {
            events.deleteAllInBatch();
            accruals.deleteAllInBatch();
            schedules.deleteAllInBatch();
            loans.deleteAllInBatch();
            Loan loan = Loan.create("TX-LOAN", 1L, "KRW", Loan.LoanType.TERM_LOAN,
                    new BigDecimal("12000000"), new BigDecimal("0.06"), DATE.minusMonths(1),
                    DATE.plusMonths(11), Loan.PaymentFrequency.MONTHLY, "SYSTEM");
            loan.activateAfterDisbursal(loan.getDisbursalDate(), loan.getPrincipalAmount(), "SYSTEM");
            loans.saveAndFlush(loan);
            EIRAmortizationSchedule schedule = new EIRAmortizationSchedule();
            schedule.setLoan(loan);
            schedule.setScheduleDate(DATE);
            schedule.setBeginningBalance(new BigDecimal("12000000"));
            schedule.setPrincipalRepayment(new BigDecimal("1000000"));
            schedule.setInterestIncome(new BigDecimal("60000"));
            schedule.setCashFlow(new BigDecimal("1060000"));
            schedule.setEndingBalance(new BigDecimal("11000000"));
            schedules.save(schedule);
            LoanAccrualLog log = LoanAccrualLog.start(loan, DATE, new BigDecimal("60000"), "SYSTEM");
            log.markSuccess(20L, "ACCRUAL-20");
            accruals.save(log);
            return loan.getId();
        });
    }

    @Test
    void remoteFailureRetainsCommittedReservationAndRetryDoesNotPostAgain() {
        when(journal.post(any())).thenAnswer(invocation -> {
            assertCommittedReservation();
            throw new IllegalStateException("Remote response lost");
        });

        assertThatThrownBy(() -> service.processIndividualRepayment(loanId, DATE))
                .hasMessageContaining("Remote response lost");

        assertCommittedReservation();
        assertThatThrownBy(() -> service.processIndividualRepayment(loanId, DATE))
                .hasMessageContaining("manual journal reconciliation");
        assertCommittedReservation();
        verify(journal, times(1)).post(any());
    }

    @Test
    void normalCompletionCommitsOneEventAndRepaymentAndRepeatDoesNotPostAgain() {
        when(journal.post(any())).thenAnswer(invocation -> {
            assertCommittedReservation();
            return new LoanJournalPort.PostedJournal(40L, "REPAYMENT-40");
        });

        assertThat(service.processIndividualRepayment(loanId, DATE))
                .isEqualTo(ScheduledRepaymentService.RepaymentResult.SUCCESS);
        assertThat(service.processIndividualRepayment(loanId, DATE))
                .isEqualTo(ScheduledRepaymentService.RepaymentResult.ALREADY_SUCCESSFUL);

        assertThat(eventCount()).isEqualTo(1);
        assertThat(eventType()).isEqualTo("SCHEDULED_REPAYMENT");
        assertThat(jdbc.queryForObject("select journal_entry_id from loan_events where loan_id = ?",
                Long.class, loanId)).isEqualTo(40L);
        assertThat(jdbc.queryForObject("select journal_entry_slip_no from loan_events where loan_id = ?",
                String.class, loanId)).isEqualTo("REPAYMENT-40");
        assertThat(amount("current_principal_balance")).isEqualByComparingTo("11000000");
        assertThat(amount("total_principal_paid")).isEqualByComparingTo("1000000");
        assertThat(amount("total_interest_paid")).isEqualByComparingTo("60000");
        assertThat(amount("current_eir")).isEqualByComparingTo("0.06");
        verify(journal, times(1)).post(any());
    }

    @Test
    void finalizationFailureRollsBackLoanAndCompletionWhilePreservingReservation() {
        when(journal.post(any())).thenAnswer(invocation -> {
            assertCommittedReservation();
            return new LoanJournalPort.PostedJournal(40L, "REPAYMENT-40");
        });
        doAnswer(invocation -> {
            LoanEvent event = invocation.getArgument(0);
            if (event.getEventType() == LoanEvent.EventType.SCHEDULED_REPAYMENT) {
                // Force actual UPDATE statements before failure to prove database rollback.
                events.flush();
                throw new IllegalStateException("Finalization persistence failed");
            }
            return invocation.callRealMethod();
        }).when(repayment).saveEvent(any());

        assertThatThrownBy(() -> service.processIndividualRepayment(loanId, DATE))
                .hasMessageContaining("Finalization persistence failed");

        assertCommittedReservation();
        assertThatThrownBy(() -> service.processIndividualRepayment(loanId, DATE))
                .hasMessageContaining("manual journal reconciliation");
        assertCommittedReservation();
        verify(journal, times(1)).post(any());
    }

    private void assertCommittedReservation() {
        // The JDBC reads acquire another connection outside a service transaction.
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(eventCount()).isEqualTo(1);
        assertThat(eventType()).isEqualTo("SCHEDULED_REPAYMENT_PENDING");
        assertThat(jdbc.queryForObject("select journal_entry_id from loan_events where loan_id = ?",
                Long.class, loanId)).isNull();
        assertThat(amount("current_principal_balance")).isEqualByComparingTo("12000000");
        assertThat(amount("total_principal_paid")).isZero();
        assertThat(amount("total_interest_paid")).isZero();
    }

    private Integer eventCount() {
        return jdbc.queryForObject("select count(*) from loan_events where loan_id = ?", Integer.class, loanId);
    }

    private String eventType() {
        return jdbc.queryForObject("select event_type from loan_events where loan_id = ?", String.class, loanId);
    }

    private BigDecimal amount(String column) {
        return jdbc.queryForObject("select " + column + " from loans where id = ?", BigDecimal.class, loanId);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = LoanRepository.class)
    @Import({ScheduledRepaymentService.class, JpaLoanAccrualPersistenceAdapter.class})
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:scheduled_repayment_tx;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan(Loan.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of(
                    "hibernate.hbm2ddl.auto", "create-drop",
                    "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
            return factory;
        }

        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean JpaLoanRepaymentPersistenceAdapter repayment(LoanEventRepository events, LoanRepository loans) {
            return spy(new JpaLoanRepaymentPersistenceAdapter(events, loans));
        }

        @Bean LoanJournalPort journal() { return mock(LoanJournalPort.class); }
        @Bean LoanReferenceDataPort reference() { return mock(LoanReferenceDataPort.class); }

        @Bean LoanAccountingProperties properties() {
            var properties = new LoanAccountingProperties();
            properties.setCashAccountCode("cash");
            properties.setLoanReceivableAccountCode("principal");
            properties.setAccruedInterestReceivableAccountCode("accrued");
            return properties;
        }
    }
}
