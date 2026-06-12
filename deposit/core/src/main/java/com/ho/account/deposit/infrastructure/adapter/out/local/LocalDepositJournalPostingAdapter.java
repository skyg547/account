package com.ho.account.deposit.infrastructure.adapter.out.local;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local-only journal posting adapter for deposit standalone execution.
 *
 * <p>초기입금 전표 생성 흐름을 로컬에서 확인하기 위한 어댑터입니다. 실제 회계 전기는
 * journal-ledger 모듈의 어댑터로 교체해야 하며, 이 구현은 별도 저장소에 전표를 쓰지 않고
 * 생성된 것처럼 식별자만 반환합니다.</p>
 */
@Component
@ConditionalOnProperty(prefix = "account.deposit.local-adapters", name = "enabled", havingValue = "true")
public class LocalDepositJournalPostingAdapter implements JournalPostingPort {

    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
        long id = sequence.getAndIncrement();
        return new JournalPostingResult(id, "LOCAL-DEP-" + id, "DRAFT");
    }
}
