package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.application.port.out.BudgetFiscalYearControlPersistencePort;
import com.ho.account.budget.domain.BudgetFiscalYearControl;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 회계연도 제어 Port의 JPA 구현입니다.
 *
 * <p>{@code save}는 새 행을 만들지 않습니다. 지원 가능한 모든 네 자리 연도는 V50에
 * 존재해야 하며, 누락은 잠금 안전 계약이 깨진 배포 오류이므로 즉시 실패합니다.</p>
 */
@Component
public class JpaBudgetFiscalYearControlPersistenceAdapter
        implements BudgetFiscalYearControlPersistencePort {

    private final SpringDataBudgetFiscalYearControlRepository repository;
    private final BudgetFiscalYearControlPersistenceMapper mapper;

    public JpaBudgetFiscalYearControlPersistenceAdapter(
            SpringDataBudgetFiscalYearControlRepository repository,
            BudgetFiscalYearControlPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<BudgetFiscalYearControl> findByFiscalYearForUpdate(String fiscalYear) {
        return repository.findByFiscalYearForUpdate(fiscalYear).map(mapper::toDomain);
    }

    @Override
    public BudgetFiscalYearControl save(BudgetFiscalYearControl control) {
        BudgetFiscalYearControlJpaEntity entity = repository.findById(control.fiscalYear())
                .orElseThrow(() -> new IllegalStateException(
                        "사전 생성된 회계연도 제어 행이 없습니다: " + control.fiscalYear()));
        mapper.copy(control, entity);
        return BudgetPersistenceConflictTranslator.translate(
                "회계연도 제어 상태가 동시에 변경되었습니다: " + control.fiscalYear(),
                () -> mapper.toDomain(repository.saveAndFlush(entity)));
    }
}
