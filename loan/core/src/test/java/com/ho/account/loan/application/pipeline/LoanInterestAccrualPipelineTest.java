package com.ho.account.loan.application.pipeline;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.service.InterestAccrualService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanInterestAccrualPipelineTest {

    @Mock private InterestAccrualService interestAccrualService;

    @Test
    void chunkKeepsPerLoanProcessingOrderAndFailsAfterCollectingFailures() {
        LocalDate date = LocalDate.of(2026, 5, 12);
        Loan first = loan(1L);
        Loan second = loan(2L);
        Loan third = loan(3L);
        when(interestAccrualService.processIndividualAccrual(1L, date))
                .thenReturn(InterestAccrualService.AccrualResult.SUCCESS);
        when(interestAccrualService.processIndividualAccrual(2L, date))
                .thenReturn(InterestAccrualService.AccrualResult.FAILED);
        when(interestAccrualService.processIndividualAccrual(3L, date))
                .thenReturn(InterestAccrualService.AccrualResult.ALREADY_SUCCESSFUL);

        LoanInterestAccrualPipeline pipeline = new LoanInterestAccrualPipeline(interestAccrualService);

        assertThatThrownBy(() -> pipeline.processChunk(List.of(first, second, third), date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("loanIds=[2]");
        InOrder order = Mockito.inOrder(interestAccrualService);
        order.verify(interestAccrualService).processIndividualAccrual(1L, date);
        order.verify(interestAccrualService).processIndividualAccrual(2L, date);
        order.verify(interestAccrualService).processIndividualAccrual(3L, date);
    }

    @Test
    void persistedLoanIdIsRequiredBeforeAnyBusinessCall() {
        Loan transientLoan = new Loan();
        LoanInterestAccrualPipeline pipeline = new LoanInterestAccrualPipeline(interestAccrualService);

        assertThatThrownBy(() -> pipeline.processChunk(
                List.of(transientLoan), LocalDate.of(2026, 5, 12)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("persisted loan");
        verify(interestAccrualService, Mockito.never())
                .processIndividualAccrual(Mockito.any(), Mockito.any());
    }

    private Loan loan(Long id) {
        Loan loan = new Loan();
        loan.setId(id);
        return loan;
    }
}
