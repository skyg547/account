package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.application.port.out.BudgetPlanPersistencePort;
import com.ho.account.budget.domain.BudgetPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 예산 Port를 Spring Data에 연결합니다.
 *
 * <p>수정 저장은 새 엔티티를 merge하지 않고 같은 영속 행을 다시 찾아 복사합니다.
 * 이렇게 해야 {@code @Version}, 생성 시각, 현재 트랜잭션의 비관적 잠금이 보존됩니다.</p>
 */
@Component
public class JpaBudgetPlanPersistenceAdapter implements BudgetPlanPersistencePort {

    private final SpringDataBudgetPlanRepository repository;
    private final BudgetPlanPersistenceMapper mapper;

    public JpaBudgetPlanPersistenceAdapter(
            SpringDataBudgetPlanRepository repository,
            BudgetPlanPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public BudgetPlan save(BudgetPlan plan) {
        BudgetPlanJpaEntity entity = plan.id() == null
                ? mapper.newEntity(plan)
                : repository.findById(plan.id())
                        .orElseThrow(() -> new IllegalStateException(
                                "수정할 예산 행을 찾을 수 없습니다: " + plan.id()));
        if (plan.id() != null) {
            mapper.copy(plan, entity);
        }
        /*
         * save의 반환값이 실제 DB snapshot이라는 Port 계약과 즉시 충돌 감지를 위해 flush합니다.
         * 연말 마감의 행별 flush 비용은 현재 단건 Port의 한계이므로 성능 위험으로 별도 보고합니다.
         */
        return BudgetPersistenceConflictTranslator.translate(
                "예산 업무키 또는 버전이 동시에 충돌했습니다: " + plan.planCode(),
                () -> mapper.toDomain(repository.saveAndFlush(entity)));
    }

    @Override
    public Optional<BudgetPlan> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetPlan> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetPlan> findByPlanCode(String planCode) {
        return repository.findByPlanCode(planCode).map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetPlan> findByBusinessKey(
            String yearMonth, String departmentCode, String accountCode) {
        return repository
                .findByYearMonthAndDepartmentCodeAndAccountCode(
                        yearMonth, departmentCode, accountCode)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetPlan> findByBusinessKeyForUpdate(
            String yearMonth, String departmentCode, String accountCode) {
        return repository
                .findByBusinessKeyForUpdate(yearMonth, departmentCode, accountCode)
                .map(mapper::toDomain);
    }

    @Override
    public List<BudgetPlan> findApprovedByFiscalYearForUpdateOrderById(String fiscalYear) {
        return repository.findApprovedByFiscalYearForUpdateOrderById(fiscalYear).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
