package com.salescrm.web.rest;

import com.salescrm.security.SecurityUtils;
import com.salescrm.service.AttendanceVerificationService;
import com.salescrm.service.dto.StaffAttendanceDTO;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Fingerprint-verified check-in.
 * <ul>
 *   <li>{@code POST /api/attendance/verification} start a check-in (web or app)</li>
 *   <li>{@code GET  /api/attendance/verification/pending} newest open request (phone picks it up)</li>
 *   <li>{@code GET  /api/attendance/verification/{id}} status (web polls while waiting)</li>
 *   <li>{@code POST /api/attendance/verification/{id}/approve} phone sends its fingerprint signature</li>
 *   <li>{@code POST /api/attendance/verification/{id}/reject} "this wasn't me"</li>
 * </ul>
 * Every call acts only for the logged-in user.
 */
@RestController
@RequestMapping("/api/attendance/verification")
public class AttendanceVerificationResource {

    private final AttendanceVerificationService verificationService;

    public AttendanceVerificationResource(AttendanceVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    private static Mono<String> currentUser() {
        return SecurityUtils.getCurrentUserLogin()
            .filter(login -> !login.isBlank() && !"anonymousUser".equalsIgnoreCase(login))
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required.")));
    }

    @PostMapping("")
    public Mono<ResponseEntity<Map<String, Object>>> startCheckIn(
        @RequestBody(required = false) Map<String, String> body,
        ServerHttpRequest request
    ) {
        Map<String, String> payload = body != null ? body : Map.of();
        String requestedFrom = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        return currentUser()
            .flatMap(user ->
                verificationService.createCheckInChallenge(
                    user,
                    payload.get("source"),
                    payload.get("workMode"),
                    payload.get("note"),
                    requestedFrom
                )
            )
            .map(view -> ResponseEntity.status(HttpStatus.CREATED).body(view));
    }

    @GetMapping("/pending")
    public Mono<ResponseEntity<Map<String, Object>>> getPending() {
        return currentUser()
            .flatMap(verificationService::getPending)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getStatus(@PathVariable Long id) {
        return currentUser().flatMap(user -> verificationService.getForOwner(id, user)).map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/approve")
    public Mono<ResponseEntity<StaffAttendanceDTO>> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return currentUser()
            .flatMap(user -> verificationService.approve(id, user, body.get("signature"), body.get("deviceId")))
            .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(result));
    }

    @PostMapping("/{id}/reject")
    public Mono<ResponseEntity<Map<String, Object>>> reject(@PathVariable Long id) {
        return currentUser().flatMap(user -> verificationService.reject(id, user)).map(ResponseEntity::ok);
    }
}
