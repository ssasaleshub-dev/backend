package com.salescrm.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class LeadPreferenceSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("mattress_type", table, columnPrefix + "_mattress_type"));
        columns.add(Column.aliased("mattress_size", table, columnPrefix + "_mattress_size"));
        columns.add(Column.aliased("budget_range", table, columnPrefix + "_budget_range"));
        columns.add(Column.aliased("purchase_timeline", table, columnPrefix + "_purchase_timeline"));
        columns.add(Column.aliased("created_at", table, columnPrefix + "_created_at"));

        columns.add(Column.aliased("lead_id", table, columnPrefix + "_lead_id"));
        return columns;
    }
}
