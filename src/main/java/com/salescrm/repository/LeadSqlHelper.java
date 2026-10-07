package com.salescrm.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class LeadSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("external_id", table, columnPrefix + "_external_id"));
        columns.add(Column.aliased("created_time", table, columnPrefix + "_created_time"));
        columns.add(Column.aliased("is_organic", table, columnPrefix + "_is_organic"));
        columns.add(Column.aliased("platform", table, columnPrefix + "_platform"));
        columns.add(Column.aliased("status", table, columnPrefix + "_status"));
        columns.add(Column.aliased("notes", table, columnPrefix + "_notes"));
        columns.add(Column.aliased("created_at", table, columnPrefix + "_created_at"));
        columns.add(Column.aliased("updated_at", table, columnPrefix + "_updated_at"));

        columns.add(Column.aliased("customer_id", table, columnPrefix + "_customer_id"));
        columns.add(Column.aliased("ad_id", table, columnPrefix + "_ad_id"));
        columns.add(Column.aliased("lead_form_id", table, columnPrefix + "_lead_form_id"));
        return columns;
    }
}
