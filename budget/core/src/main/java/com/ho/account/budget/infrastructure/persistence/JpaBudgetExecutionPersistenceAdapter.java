package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.application.port.out.BudgetExecutionPersistencePort;
import com.ho.account.budget.domain.BudgetExecution;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaBudgetExecutionPersistenceAdapter implements BudgetExecutionPersistencePort {

    private final SpringDataBudgetExecutionRepository repository;
    private final BudgetExecutionPersistenceMapper mapper;

    public JpaBudgetExecutionPersistenceAdapter(
            SpringDataBudgetExecutionRepository repository,
            BudgetExecutionPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public BudgetExecution save(BudgetExecution execution) {
        BudgetExecutionJpaEntity entity = execution.id() == null
                ? mapper.newEntity(execution)
                : repository.findById(execution.id())
                        .orElseThrow(() -> new IllegalStateException(
                                "수정할 예산 집행 행을 찾을 수 없습니다: " + execution.id()));
        if (execution.id() != null) {
            mapper.copy(execution, entity);
        }
        return BudgetPersistenceConflictTranslator.translate(
                "예산 집행 원천키가 동시에 충돌했습니다: "
                        + execution.sourceType()
                        + "/"
                        + execution.sourceId()
                        + "/"
                        + execution.sourceLineId(),
                () -> mapper.toDomain(repository.saveAndFlush(entity)));
    }

    @Override
    public Optional<BudgetExecution> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetExecution> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetExecution> findBySource(
            String sourceType, String sourceId, String sourceLineId) {
        return repository
                .findBySourceTypeAndSourceIdAndSourceLineId(sourceType, sourceId, sourceLineId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetExecution> findBySourceForUpdate(
            String sourceType, String sourceId, String sourceLineId) {
        return repository.findBySourceForUpdate(sourceType, sourceId, sourceLineId)
                .map(mapper::toDomain);
    }
}
