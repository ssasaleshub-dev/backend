package com.salescrm.repository;

import com.salescrm.domain.Ad;
import com.salescrm.repository.rowmapper.AdRowMapper;
import com.salescrm.repository.rowmapper.AdSetRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the Ad entity.
 */
@SuppressWarnings("unused")
class AdRepositoryInternalImpl extends SimpleR2dbcRepository<Ad, Long> implements AdRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final AdSetRowMapper adsetMapper;
    private final AdRowMapper adMapper;

    private static final Table entityTable = Table.aliased("ad", EntityManager.ENTITY_ALIAS);
    private static final Table adSetTable = Table.aliased("ad_set", "adSet");

    public AdRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        AdSetRowMapper adsetMapper,
        AdRowMapper adMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Ad.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.adsetMapper = adsetMapper;
        this.adMapper = adMapper;
    }

    @Override
    public Flux<Ad> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Ad> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = AdSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(AdSetSqlHelper.getColumns(adSetTable, "adSet"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(adSetTable)
            .on(Column.create("ad_set_id", entityTable))
            .equals(Column.create("id", adSetTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Ad.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Ad> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Ad> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<Ad> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Ad> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Ad> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Ad process(Row row, RowMetadata metadata) {
        Ad entity = adMapper.apply(row, "e");
        entity.setAdSet(adsetMapper.apply(row, "adSet"));
        return entity;
    }

    @Override
    public <S extends Ad> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
