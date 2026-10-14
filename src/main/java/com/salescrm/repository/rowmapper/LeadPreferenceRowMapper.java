package com.salescrm.repository.rowmapper;

import com.salescrm.domain.LeadPreference;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link LeadPreference}, with proper type conversions.
 */
@Service
public class LeadPreferenceRowMapper implements BiFunction<Row, String, LeadPreference> {

    private final ColumnConverter converter;

    public LeadPreferenceRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link LeadPreference} stored in the database.
     */
    @Override
    public LeadPreference apply(Row row, String prefix) {
        LeadPreference entity = new LeadPreference();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setMattressType(converter.fromRow(row, prefix + "_mattress_type", String.class));
        entity.setMattressSize(converter.fromRow(row, prefix + "_mattress_size", String.class));
        entity.setBudgetRange(converter.fromRow(row, prefix + "_budget_range", String.class));
        entity.setPurchaseTimeline(converter.fromRow(row, prefix + "_purchase_timeline", String.class));
        entity.setVisaType(converter.fromRow(row, prefix + "_visa_type", String.class));
        entity.setAge(converter.fromRow(row, prefix + "_age", String.class));
        entity.setEducationalQualification(converter.fromRow(row, prefix + "_educational_qualification", String.class));
        entity.setAreYouInterested(converter.fromRow(row, prefix + "_are_you_interested", String.class));
        entity.setAreYouWorking(converter.fromRow(row, prefix + "_are_you_working", String.class));
        entity.setLeadConfirm(converter.fromRow(row, prefix + "_lead_confirm", String.class));
        entity.setAssignedTo(converter.fromRow(row, prefix + "_assigned_to", String.class));
        entity.setCreatedAt(converter.fromRow(row, prefix + "_created_at", Instant.class));
        entity.setLeadId(converter.fromRow(row, prefix + "_lead_id", Long.class));
        return entity;
    }
}
