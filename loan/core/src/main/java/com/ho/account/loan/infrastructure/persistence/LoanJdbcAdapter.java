package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.application.port.out.LoanPort;
import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import com.ho.account.loan.domain.LoanContract;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * JDBC 기반??고성???�?�량 처리 ?�댑?? */
@Repository
@RequiredArgsConstructor
public class LoanJdbcAdapter implements LoanPort {

    private final JdbcTemplate jdbcTemplate;
    private final LoanContractRepository jpaRepository;

    @Override
    public void saveLoanContract(LoanContract loanContract) {
        jpaRepository.save(loanContract);
    }

    @Override
    @Transactional
    public void saveAllAmortizationEntries(List<LoanAmortizationScheduleEntry> entries) {
        String sql = "INSERT INTO LOAN_AMORTIZATION_SCHEDULE_ENTRIES " +
                     "(LOAN_CONTRACT_ID, PAYMENT_DATE, PERIOD_NUMBER, STARTING_BALANCE, " +
                     "SCHEDULED_PAYMENT_AMOUNT, INTEREST_AMOUNT, PRINCIPAL_AMOUNT, ENDING_BALANCE, ENTRY_TYPE, CREATE_DATE, UPDATE_DATE, AUDIT_USER) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM')";

        jdbcTemplate.batchUpdate(sql, entries, 1000, (PreparedStatement ps, LoanAmortizationScheduleEntry entry) -> {
            ps.setLong(1, entry.getLoanContract().getId());
            ps.setDate(2, Date.valueOf(entry.getPaymentDate()));
            ps.setInt(3, entry.getPeriodNumber());
            ps.setBigDecimal(4, entry.getStartingBalance());
            ps.setBigDecimal(5, entry.getScheduledPaymentAmount());
            ps.setBigDecimal(6, entry.getInterestAmount());
            ps.setBigDecimal(7, entry.getPrincipalAmount());
            ps.setBigDecimal(8, entry.getEndingBalance());
            ps.setString(9, entry.getEntryType());
        });
    }

    @Override
    public LoanContract findByContractNo(String contractNo) {
        return jpaRepository.findByLoanContractNo(contractNo)
                .orElseThrow(() -> new IllegalArgumentException("Loan contract not found: " + contractNo));
    }
}
