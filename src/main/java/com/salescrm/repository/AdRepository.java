package com.salescrm.repository;

import com.salescrm.domain.Ad;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Ad entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AdRepository extends ReactiveCrudRepository<Ad, Long>, AdRepositoryInternal {
    Flux<Ad> findAllBy(Pageable pageable);

    Mono<Ad> findByExternalId(String externalId);

    @Override
    Mono<Ad> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Ad> findAllWithEagerRelationships();

    @Override
    Flux<Ad> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM ad entity WHERE entity.ad_set_id = :id")
    Flux<Ad> findByAdSet(Long id);

    @Query("SELECT * FROM ad entity WHERE entity.ad_set_id IS NULL")
    Flux<Ad> findAllWhereAdSetIsNull();

    @Override
    <S extends Ad> Mono<S> save(S entity);

    @Override
    Flux<Ad> findAll();

    @Override
    Mono<Ad> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface AdRepositoryInternal {
    <S extends Ad> Mono<S> save(S entity);

    Flux<Ad> findAllBy(Pageable pageable);

    Flux<Ad> findAll();

    Mono<Ad> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Ad> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Ad> findOneWithEagerRelationships(Long id);

    Flux<Ad> findAllWithEagerRelationships();

    Flux<Ad> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
