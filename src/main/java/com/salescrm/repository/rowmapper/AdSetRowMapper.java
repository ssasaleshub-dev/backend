package com.salescrm.repository.rowmapper;

import com.salescrm.domain.AdSet;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link AdSet}, with proper type conversions.
 */
@Service
public class AdSetRowMapper implements BiFunction<Row, String, AdSet> {

    private final ColumnConverter converter;

    public AdSetRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link AdSet} stored in the database.
     */
    @Override
    public AdSet apply(Row row, String prefix) {
        AdSet entity = new AdSet();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setExternalId(converter.fromRow(row, prefix + "_external_id", String.class));
        entity.setName(converter.fromRow(row, prefix + "_name", String.class));
        entity.setStatus(converter.fromRow(row, prefix + "_status", String.class));
        entity.setCreatedAt(converter.fromRow(row, prefix + "_created_at", Instant.class));
        entity.setCampaignId(converter.fromRow(row, prefix + "_campaign_id", Long.class));
        return entity;
    }
}
