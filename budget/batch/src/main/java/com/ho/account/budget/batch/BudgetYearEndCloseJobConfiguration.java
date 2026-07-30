package com.ho.account.budget.batch;

import com.ho.account.budget.application.port.in.BudgetYearEndUseCase;
import java.util.regex.Pattern;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 예산 회계연도 마감 Job의 실행 순서와 파라미터 경계만 담당한다.
 *
 * <p>식별 파라미터인 {@code fiscalYear}에는 정확히 네 자리 연도(예: {@code 2026})가 필요하다.
 * {@code actor}는 선택적인 감사 정보이며 없으면 {@code SYSTEM}을 사용한다. 운영 launcher에서는
 * actor를 non-identifying 파라미터로 전달해야 같은 회계연도가 하나의 JobInstance로 유지된다.
 *
 * <p>증분 실행자를 사용하지 않으므로 성공한 회계연도는 실수로 다시 실행되지 않는다. 실패한 인스턴스는
 * 같은 fiscalYear로 재시작할 수 있으며, core 유즈케이스의 예외는 삼키지 않아 Step과 Job이 FAILED가 된다.
 * Tasklet 호출은 Step 트랜잭션 안에서 실행되고 실제 상태 전이와 멱등성 규칙은 core가 소유한다.
 *
 * <p>현재 규모 가정은 회계연도 하나를 단일 트랜잭션으로 순차 마감하는 것이다. 따라서 이 Job에는
 * chunk나 partition을 임의로 넣지 않는다. 연도별 데이터가 한 트랜잭션 한계를 넘는 시점에는 core가
 * 재시작 가능한 페이지/체크포인트 계약을 먼저 제공한 뒤 Batch 병렬성을 확장해야 한다.
 */
@Configuration(proxyBeanMethods = false)
public class BudgetYearEndCloseJobConfiguration {

    public static final String JOB_NAME = "budgetYearEndCloseJob";
    public static final String STEP_NAME = "budgetYearEndCloseStep";
    static final String FISCAL_YEAR_PARAMETER = "fiscalYear";
    static final String ACTOR_PARAMETER = "actor";
    static final String DEFAULT_ACTOR = "SYSTEM";

    private static final Pattern FOUR_DIGIT_YEAR = Pattern.compile("[0-9]{4}");

    @Bean
    public Job budgetYearEndCloseJob(
            JobRepository jobRepository,
            Step budgetYearEndCloseStep,
            JobParametersValidator budgetYearEndCloseJobParametersValidator) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(budgetYearEndCloseJobParametersValidator)
                .start(budgetYearEndCloseStep)
                .build();
    }

    @Bean
    public Step budgetYearEndCloseStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet budgetYearEndCloseTasklet) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(budgetYearEndCloseTasklet, transactionManager)
                .build();
    }

    @Bean
    public JobParametersValidator budgetYearEndCloseJobParametersValidator() {
        return parameters -> requireFiscalYear(parameters);
    }

    @Bean
    public Tasklet budgetYearEndCloseTasklet(BudgetYearEndUseCase budgetYearEndUseCase) {
        return (contribution, chunkContext) -> {
            JobParameters parameters = contribution.getStepExecution().getJobParameters();
            String fiscalYear = requireFiscalYear(parameters);
            String actor = optionalActor(parameters);

            // 마감 가능 여부와 상태 전이는 core 유즈케이스가 판단한다. Batch는 실행과 관측만 담당한다.
            int closedBudgetCount = budgetYearEndUseCase.closeFiscalYear(fiscalYear, actor);
            contribution.incrementWriteCount(closedBudgetCount);
            return RepeatStatus.FINISHED;
        };
    }

    static String requireFiscalYear(JobParameters parameters) throws JobParametersInvalidException {
        String value = parameters == null ? null : parameters.getString(FISCAL_YEAR_PARAMETER);
        if (value == null || !FOUR_DIGIT_YEAR.matcher(value).matches()) {
            throw new JobParametersInvalidException(
                    "fiscalYear job parameter is required and must contain exactly four digits.");
        }
        return value;
    }

    static String optionalActor(JobParameters parameters) {
        String value = parameters == null ? null : parameters.getString(ACTOR_PARAMETER);
        return value == null || value.isBlank() ? DEFAULT_ACTOR : value.trim();
    }
}
