package com.salescrm.repository;

import com.salescrm.domain.LeadPreference;
import com.salescrm.repository.rowmapper.LeadPreferenceRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the LeadPreference entity.
 */
@SuppressWarnings("unused")
class LeadPreferenceRepositoryInternalImpl extends SimpleR2dbcRepository<LeadPreference, Long> implements LeadPreferenceRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final LeadRowMapper leadMapper;
    private final LeadPreferenceRowMapper leadpreferenceMapper;

    private static final Table entityTable = Table.aliased("lead_preference", EntityManager.ENTITY_ALIAS);
    private static final Table leadTable = Table.aliased("lead", "e_lead");

    public LeadPreferenceRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        LeadRowMapper leadMapper,
        LeadPreferenceRowMapper leadpreferenceMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(LeadPreference.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.leadMapper = leadMapper;
        this.leadpreferenceMapper = leadpreferenceMapper;
    }

    @Override
    public Flux<LeadPreference> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<LeadPreference> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = LeadPreferenceSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(LeadSqlHelper.getColumns(leadTable, "lead"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(leadTable)
            .on(Column.create("lead_id", entityTable))
            .equals(Column.create("id", leadTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, LeadPreference.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<LeadPreference> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<LeadPreference> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<LeadPreference> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<LeadPreference> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<LeadPreference> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private LeadPreference process(Row row, RowMetadata metadata) {
        LeadPreference entity = leadpreferenceMapper.apply(row, "e");
        entity.setLead(leadMapper.apply(row, "lead"));
        return entity;
    }

    @Override
    public <S extends LeadPreference> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
