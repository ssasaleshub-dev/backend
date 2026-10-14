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
        columns.add(Column.aliased("visa_type", table, columnPrefix + "_visa_type"));
        columns.add(Column.aliased("age", table, columnPrefix + "_age"));
        columns.add(Column.aliased("educational_qualification", table, columnPrefix + "_educational_qualification"));
        columns.add(Column.aliased("are_you_interested", table, columnPrefix + "_are_you_interested"));
        columns.add(Column.aliased("are_you_working", table, columnPrefix + "_are_you_working"));
        columns.add(Column.aliased("lead_confirm", table, columnPrefix + "_lead_confirm"));
        columns.add(Column.aliased("assigned_to", table, columnPrefix + "_assigned_to"));
        columns.add(Column.aliased("created_at", table, columnPrefix + "_created_at"));

        columns.add(Column.aliased("lead_id", table, columnPrefix + "_lead_id"));
        return columns;
    }
}
