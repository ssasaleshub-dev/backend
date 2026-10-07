package com.salescrm.repository.rowmapper;

import com.salescrm.domain.Campaign;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Campaign}, with proper type conversions.
 */
@Service
public class CampaignRowMapper implements BiFunction<Row, String, Campaign> {

    private final ColumnConverter converter;

    public CampaignRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Campaign} stored in the database.
     */
    @Override
    public Campaign apply(Row row, String prefix) {
        Campaign entity = new Campaign();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setExternalId(converter.fromRow(row, prefix + "_external_id", String.class));
        entity.setName(converter.fromRow(row, prefix + "_name", String.class));
        entity.setStatus(converter.fromRow(row, prefix + "_status", String.class));
        entity.setCreatedAt(converter.fromRow(row, prefix + "_created_at", Instant.class));
        entity.setUpdatedAt(converter.fromRow(row, prefix + "_updated_at", Instant.class));
        return entity;
    }
}
