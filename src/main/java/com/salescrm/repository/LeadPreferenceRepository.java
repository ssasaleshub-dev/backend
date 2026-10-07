package com.salescrm.repository;

import com.salescrm.domain.LeadPreference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the LeadPreference entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LeadPreferenceRepository extends ReactiveCrudRepository<LeadPreference, Long>, LeadPreferenceRepositoryInternal {
    @Override
    Mono<LeadPreference> findOneWithEagerRelationships(Long id);

    @Override
    Flux<LeadPreference> findAllWithEagerRelationships();

    @Override
    Flux<LeadPreference> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM lead_preference entity WHERE entity.lead_id = :id")
    Flux<LeadPreference> findByLead(Long id);

    @Query("SELECT * FROM lead_preference entity WHERE entity.lead_id = :id LIMIT 1")
    Mono<LeadPreference> findOneByLeadId(Long id);

    @Query("SELECT * FROM lead_preference entity WHERE entity.lead_id IS NULL")
    Flux<LeadPreference> findAllWhereLeadIsNull();

    @Override
    <S extends LeadPreference> Mono<S> save(S entity);

    @Override
    Flux<LeadPreference> findAll();

    @Override
    Mono<LeadPreference> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface LeadPreferenceRepositoryInternal {
    <S extends LeadPreference> Mono<S> save(S entity);

    Flux<LeadPreference> findAllBy(Pageable pageable);

    Flux<LeadPreference> findAll();

    Mono<LeadPreference> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<LeadPreference> findAllBy(Pageable pageable, Criteria criteria);

    Mono<LeadPreference> findOneWithEagerRelationships(Long id);

    Flux<LeadPreference> findAllWithEagerRelationships();

    Flux<LeadPreference> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
