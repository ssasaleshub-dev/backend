package com.salescrm.repository.rowmapper;

import com.salescrm.domain.Customer;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Customer}, with proper type conversions.
 */
@Service
public class CustomerRowMapper implements BiFunction<Row, String, Customer> {

    private final ColumnConverter converter;

    public CustomerRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Customer} stored in the database.
     */
    @Override
    public Customer apply(Row row, String prefix) {
        Customer entity = new Customer();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setFullName(converter.fromRow(row, prefix + "_full_name", String.class));
        entity.setPhoneNumber(converter.fromRow(row, prefix + "_phone_number", String.class));
        entity.setRawPhoneNumber(converter.fromRow(row, prefix + "_raw_phone_number", String.class));
        entity.setLocation(converter.fromRow(row, prefix + "_location", String.class));
        entity.setCreatedAt(converter.fromRow(row, prefix + "_created_at", Instant.class));
        return entity;
    }
}
