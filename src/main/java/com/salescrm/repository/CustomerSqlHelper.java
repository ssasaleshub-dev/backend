package com.salescrm.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class CustomerSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("full_name", table, columnPrefix + "_full_name"));
        columns.add(Column.aliased("phone_number", table, columnPrefix + "_phone_number"));
        columns.add(Column.aliased("raw_phone_number", table, columnPrefix + "_raw_phone_number"));
        columns.add(Column.aliased("location", table, columnPrefix + "_location"));
        columns.add(Column.aliased("created_at", table, columnPrefix + "_created_at"));

        return columns;
    }
}
