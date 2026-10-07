package com.salescrm.repository.rowmapper;

import com.salescrm.domain.Ad;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Ad}, with proper type conversions.
 */
@Service
public class AdRowMapper implements BiFunction<Row, String, Ad> {

    private final ColumnConverter converter;

    public AdRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Ad} stored in the database.
     */
    @Override
    public Ad apply(Row row, String prefix) {
        Ad entity = new Ad();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setExternalId(converter.fromRow(row, prefix + "_external_id", String.class));
        entity.setName(converter.fromRow(row, prefix + "_name", String.class));
        entity.setStatus(converter.fromRow(row, prefix + "_status", String.class));
        entity.setCreatedAt(converter.fromRow(row, prefix + "_created_at", Instant.class));
        entity.setAdSetId(converter.fromRow(row, prefix + "_ad_set_id", Long.class));
        return entity;
    }
}
