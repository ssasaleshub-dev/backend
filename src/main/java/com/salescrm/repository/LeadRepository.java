package com.salescrm.repository;

import com.salescrm.domain.Lead;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Lead entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LeadRepository extends ReactiveCrudRepository<Lead, Long>, LeadRepositoryInternal {
    Flux<Lead> findAllBy(Pageable pageable);

    Mono<Lead> findByExternalId(String externalId);

    @Override
    Mono<Lead> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Lead> findAllWithEagerRelationships();

    @Override
    Flux<Lead> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM lead entity WHERE entity.customer_id = :id")
    Flux<Lead> findByCustomer(Long id);

    @Query("SELECT * FROM lead entity WHERE entity.customer_id IS NULL")
    Flux<Lead> findAllWhereCustomerIsNull();

    @Query("SELECT * FROM lead entity WHERE entity.ad_id = :id")
    Flux<Lead> findByAd(Long id);

    @Query("SELECT * FROM lead entity WHERE entity.ad_id IS NULL")
    Flux<Lead> findAllWhereAdIsNull();

    @Query("SELECT * FROM lead entity WHERE entity.lead_form_id = :id")
    Flux<Lead> findByLeadForm(Long id);

    @Query("SELECT * FROM lead entity WHERE entity.lead_form_id IS NULL")
    Flux<Lead> findAllWhereLeadFormIsNull();

    @Query("SELECT * FROM lead entity WHERE entity.id not in (select lead_preference_id from lead_preference)")
    Flux<Lead> findAllWhereLeadPreferenceIsNull();

    @Override
    <S extends Lead> Mono<S> save(S entity);

    @Override
    Flux<Lead> findAll();

    @Override
    Mono<Lead> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface LeadRepositoryInternal {
    <S extends Lead> Mono<S> save(S entity);

    Flux<Lead> findAllBy(Pageable pageable);

    Flux<Lead> findAll();

    Mono<Lead> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Lead> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Lead> findOneWithEagerRelationships(Long id);

    Flux<Lead> findAllWithEagerRelationships();

    Flux<Lead> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
