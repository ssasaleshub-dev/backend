package com.salescrm.repository.rowmapper;

import com.salescrm.domain.Lead;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Lead}, with proper type conversions.
 */
@Service
public class LeadRowMapper implements BiFunction<Row, String, Lead> {

    private final ColumnConverter converter;

    public LeadRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Lead} stored in the database.
     */
    @Override
    public Lead apply(Row row, String prefix) {
        Lead entity = new Lead();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setExternalId(converter.fromRow(row, prefix + "_external_id", String.class));
        entity.setCreatedTime(converter.fromRow(row, prefix + "_created_time", Instant.class));
        entity.setIsOrganic(converter.fromRow(row, prefix + "_is_organic", Boolean.class));
        entity.setPlatform(converter.fromRow(row, prefix + "_platform", PlatformType.class));
        entity.setStatus(converter.fromRow(row, prefix + "_status", LeadStatus.class));
        entity.setNotes(converter.fromRow(row, prefix + "_notes", String.class));
        entity.setCreatedAt(converter.fromRow(row, prefix + "_created_at", Instant.class));
        entity.setUpdatedAt(converter.fromRow(row, prefix + "_updated_at", Instant.class));
        entity.setCustomerId(converter.fromRow(row, prefix + "_customer_id", Long.class));
        entity.setAdId(converter.fromRow(row, prefix + "_ad_id", Long.class));
        entity.setLeadFormId(converter.fromRow(row, prefix + "_lead_form_id", Long.class));
        return entity;
    }
}
