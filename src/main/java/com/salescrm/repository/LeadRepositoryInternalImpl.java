package com.salescrm.repository;

import com.salescrm.domain.Lead;
import com.salescrm.repository.rowmapper.AdRowMapper;
import com.salescrm.repository.rowmapper.CustomerRowMapper;
import com.salescrm.repository.rowmapper.LeadFormRowMapper;
import com.salescrm.repository.rowmapper.LeadRowMapper;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Comparison;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndJoinCondition;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC custom repository implementation for the Lead entity.
 */
@SuppressWarnings("unused")
class LeadRepositoryInternalImpl extends SimpleR2dbcRepository<Lead, Long> implements LeadRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final CustomerRowMapper customerMapper;
    private final AdRowMapper adMapper;
    private final LeadFormRowMapper leadformMapper;
    private final LeadRowMapper leadMapper;

    private static final Table entityTable = Table.aliased("lead", EntityManager.ENTITY_ALIAS);
    private static final Table customerTable = Table.aliased("customer", "customer");
    private static final Table adTable = Table.aliased("ad", "ad");
    private static final Table leadFormTable = Table.aliased("lead_form", "leadForm");

    public LeadRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        CustomerRowMapper customerMapper,
        AdRowMapper adMapper,
        LeadFormRowMapper leadformMapper,
        LeadRowMapper leadMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Lead.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.customerMapper = customerMapper;
        this.adMapper = adMapper;
        this.leadformMapper = leadformMapper;
        this.leadMapper = leadMapper;
    }

    @Override
    public Flux<Lead> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Lead> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = LeadSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(CustomerSqlHelper.getColumns(customerTable, "customer"));
        columns.addAll(AdSqlHelper.getColumns(adTable, "ad"));
        columns.addAll(LeadFormSqlHelper.getColumns(leadFormTable, "leadForm"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(customerTable)
            .on(Column.create("customer_id", entityTable))
            .equals(Column.create("id", customerTable))
            .leftOuterJoin(adTable)
            .on(Column.create("ad_id", entityTable))
            .equals(Column.create("id", adTable))
            .leftOuterJoin(leadFormTable)
            .on(Column.create("lead_form_id", entityTable))
            .equals(Column.create("id", leadFormTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Lead.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Lead> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Lead> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<Lead> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Lead> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Lead> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Lead process(Row row, RowMetadata metadata) {
        Lead entity = leadMapper.apply(row, "e");
        entity.setCustomer(customerMapper.apply(row, "customer"));
        entity.setAd(adMapper.apply(row, "ad"));
        entity.setLeadForm(leadformMapper.apply(row, "leadForm"));
        return entity;
    }

    @Override
    public <S extends Lead> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
