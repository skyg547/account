package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalRule;
import com.ho.account.journalledger.domain.journal.domain.JournalRuleDetail;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;

/**
 * JPA lifecycle boundary declared in META-INF/journal-ledger-orm.xml. The mapped domain objects
 * retain business state; this listener invokes their mutation checks on every
 * persistence path, including dirty checking, merge and ordinary deletes.
 */
public class DomainPersistenceLifecycle {
    public void beforeInsert(Object entity) {
        if (entity instanceof JournalEntry entry) entry.onCreate();
        else if (entity instanceof JournalDetail detail) detail.onCreate();
        else if (entity instanceof JournalRule rule) rule.onCreate();
        else if (entity instanceof JournalRuleDetail detail) detail.onCreate();
        else if (entity instanceof GlBalance balance) balance.onCreate();
        else if (entity instanceof SlBalance balance) balance.onCreate();
        else if (entity instanceof UnsettledItem item) item.onCreate();
    }

    public void beforeUpdate(Object entity) {
        if (entity instanceof JournalEntry entry) entry.onUpdate();
        else if (entity instanceof JournalDetail detail) detail.onUpdate();
        else if (entity instanceof JournalRule rule) rule.onUpdate();
        else if (entity instanceof JournalRuleDetail detail) detail.onUpdate();
        else if (entity instanceof GlBalance balance) balance.onUpdate();
        else if (entity instanceof SlBalance balance) balance.onUpdate();
    }

    public void beforeDelete(Object entity) {
        if (entity instanceof JournalEntry entry) entry.onRemove();
        else if (entity instanceof JournalDetail detail) detail.onRemove();
    }

    // Capture only scalar status/parent references: loading details here causes N+1 queries.
    public void afterLoad(Object entity) { capture(entity); }
    public void afterInsert(Object entity) { capture(entity); }
    public void afterUpdate(Object entity) { capture(entity); }

    private void capture(Object entity) {
        if (entity instanceof JournalEntry entry) entry.capturePersistedState();
        else if (entity instanceof JournalDetail detail) detail.capturePersistedState();
    }
}
