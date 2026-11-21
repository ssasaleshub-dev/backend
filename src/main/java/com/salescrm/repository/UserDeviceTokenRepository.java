package com.salescrm.repository;

import com.salescrm.domain.UserDeviceToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserDeviceTokenRepository extends ReactiveCrudRepository<UserDeviceToken, Long> {

    Flux<UserDeviceToken> findAllByUserId(String userId);

    Mono<UserDeviceToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM user_device_tokens WHERE token = :token")
    Mono<Long> deleteByToken(String token);

    @Modifying
    @Query("DELETE FROM user_device_tokens WHERE user_id = :userId")
    Mono<Long> deleteAllByUserId(String userId);
}
