package com.ho.account.budget.infrastructure.persistence;

import com.ho.account.budget.application.port.out.BudgetTransferPersistencePort;
import com.ho.account.budget.domain.BudgetTransfer;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaBudgetTransferPersistenceAdapter implements BudgetTransferPersistencePort {

    private final SpringDataBudgetTransferRepository repository;
    private final BudgetTransferPersistenceMapper mapper;

    public JpaBudgetTransferPersistenceAdapter(
            SpringDataBudgetTransferRepository repository,
            BudgetTransferPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public BudgetTransfer save(BudgetTransfer transfer) {
        BudgetTransferJpaEntity entity = transfer.id() == null
                ? mapper.newEntity(transfer)
                : repository.findById(transfer.id())
                        .orElseThrow(() -> new IllegalStateException(
                                "수정할 예산 전용 행을 찾을 수 없습니다: " + transfer.id()));
        if (transfer.id() != null) {
            mapper.copy(transfer, entity);
        }
        return BudgetPersistenceConflictTranslator.translate(
                "예산 전용 requestKey가 동시에 충돌했습니다: " + transfer.requestKey(),
                () -> mapper.toDomain(repository.saveAndFlush(entity)));
    }

    @Override
    public Optional<BudgetTransfer> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetTransfer> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetTransfer> findByRequestKey(String requestKey) {
        return repository.findByRequestKey(requestKey).map(mapper::toDomain);
    }

    @Override
    public Optional<BudgetTransfer> findByRequestKeyForUpdate(String requestKey) {
        return repository.findByRequestKeyForUpdate(requestKey).map(mapper::toDomain);
    }
}
