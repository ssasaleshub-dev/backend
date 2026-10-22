package com.salescrm.repository.rowmapper;

import com.salescrm.domain.ChatMessage;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link ChatMessage}, with proper type conversions.
 */
@Service
public class ChatMessageRowMapper implements BiFunction<Row, String, ChatMessage> {

    private final ColumnConverter converter;

    public ChatMessageRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link ChatMessage} stored in the database.
     */
    @Override
    public ChatMessage apply(Row row, String prefix) {
        ChatMessage entity = new ChatMessage();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setUserId(converter.fromRow(row, prefix + "_user_id", Long.class));
        entity.setContent(converter.fromRow(row, prefix + "_content", String.class));
        entity.setChannel(converter.fromRow(row, prefix + "_channel", String.class));
        entity.setMediaUrl(converter.fromRow(row, prefix + "_media_url", String.class));
        entity.setMediaType(converter.fromRow(row, prefix + "_media_type", String.class));
        entity.setFileName(converter.fromRow(row, prefix + "_file_name", String.class));
        entity.setFileSize(converter.fromRow(row, prefix + "_file_size", Long.class));
        entity.setCreatedAt(converter.fromRow(row, prefix + "_created_at", Instant.class));
        return entity;
    }
}
