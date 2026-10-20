package com.ho.account.auth.core.infrastructure.persistence;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Captures generated SQL so pagination tests can distinguish database limits from Java slicing. */
public final class SqlLimitInspector implements StatementInspector {
    static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        STATEMENTS.add(sql.toLowerCase());
        return sql;
    }
}
