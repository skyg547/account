package com.ho.account.masterdata.core.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records SQL executed by the current test thread after fixture setup. */
public class PaginationSqlInspector implements StatementInspector {
    private static final ThreadLocal<List<String>> SQL = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public String inspect(String sql) {
        SQL.get().add(sql.toLowerCase());
        return sql;
    }

    static void clear() { SQL.get().clear(); }
    static List<String> statements() { return List.copyOf(SQL.get()); }
}
