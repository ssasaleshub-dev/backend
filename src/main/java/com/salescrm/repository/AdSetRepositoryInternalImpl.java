package com.salescrm.repository;

import com.salescrm.domain.AdSet;
import com.salescrm.repository.rowmapper.AdSetRowMapper;
import com.salescrm.repository.rowmapper.CampaignRowMapper;
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
 * Spring Data R2DBC custom repository implementation for the AdSet entity.
 */
@SuppressWarnings("unused")
class AdSetRepositoryInternalImpl extends SimpleR2dbcRepository<AdSet, Long> implements AdSetRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final CampaignRowMapper campaignMapper;
    private final AdSetRowMapper adsetMapper;

    private static final Table entityTable = Table.aliased("ad_set", EntityManager.ENTITY_ALIAS);
    private static final Table campaignTable = Table.aliased("campaign", "campaign");

    public AdSetRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        CampaignRowMapper campaignMapper,
        AdSetRowMapper adsetMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(AdSet.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.campaignMapper = campaignMapper;
        this.adsetMapper = adsetMapper;
    }

    @Override
    public Flux<AdSet> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<AdSet> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = AdSetSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(CampaignSqlHelper.getColumns(campaignTable, "campaign"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(campaignTable)
            .on(Column.create("campaign_id", entityTable))
            .equals(Column.create("id", campaignTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, AdSet.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<AdSet> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<AdSet> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    @Override
    public Mono<AdSet> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<AdSet> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<AdSet> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private AdSet process(Row row, RowMetadata metadata) {
        AdSet entity = adsetMapper.apply(row, "e");
        entity.setCampaign(campaignMapper.apply(row, "campaign"));
        return entity;
    }

    @Override
    public <S extends AdSet> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
