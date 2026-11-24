package com.salescrm.repository;

import com.salescrm.domain.UserGoogleIntegration;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserGoogleIntegrationRepository extends ReactiveCrudRepository<UserGoogleIntegration, Long> {

    Mono<UserGoogleIntegration> findByUserIdAndIsActiveTrue(String userId);

    Mono<UserGoogleIntegration> findByUserId(String userId);

    @Modifying
    @Query("DELETE FROM user_google_integrations WHERE user_id = :userId")
    Mono<Long> deleteByUserId(String userId);
}

