package com.salescrm.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class ChatMessageSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("user_id", table, columnPrefix + "_user_id"));
        columns.add(Column.aliased("content", table, columnPrefix + "_content"));
        columns.add(Column.aliased("channel", table, columnPrefix + "_channel"));
        columns.add(Column.aliased("media_url", table, columnPrefix + "_media_url"));
        columns.add(Column.aliased("media_type", table, columnPrefix + "_media_type"));
        columns.add(Column.aliased("file_name", table, columnPrefix + "_file_name"));
        columns.add(Column.aliased("file_size", table, columnPrefix + "_file_size"));
        columns.add(Column.aliased("created_at", table, columnPrefix + "_created_at"));

        return columns;
    }
}
