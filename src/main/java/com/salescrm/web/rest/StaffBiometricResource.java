package com.salescrm.web.rest;

import com.salescrm.security.AuthoritiesConstants;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.StaffBiometricService;
import com.salescrm.service.dto.StaffBiometricDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing staff biometric device binding and Admin fingerprint locking.
 */
@RestController
@RequestMapping("/api/attendance/biometric")
public class StaffBiometricResource {

    private static final Logger LOG = LoggerFactory.getLogger(StaffBiometricResource.class);

    private final StaffBiometricService biometricService;

    public StaffBiometricResource(StaffBiometricService biometricService) {
        this.biometricService = biometricService;
    }

    /**
     * {@code POST /api/attendance/biometric/enroll} : Initial biometric fingerprint registration from an office Android device.
     */
    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<StaffBiometricDTO>> enrollBiometric(@Valid @RequestBody StaffBiometricDTO dto) {
        LOG.debug("REST request to enroll biometric credential: user={}, device={}", dto.getUserId(), dto.getDeviceId());
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty(dto.getUserId() != null ? dto.getUserId() : "unknown")
            .flatMap(authLogin -> {
                if (dto.getUserId() == null || dto.getUserId().isBlank()) {
                    dto.setUserId(authLogin);
                }
                return biometricService.enrollBiometric(dto);
            })
            .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(result));
    }

    /**
     * {@code GET /api/attendance/biometric/pending} : Get all pending fingerprint registrations requiring Admin approval.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public Mono<ResponseEntity<List<StaffBiometricDTO>>> getPendingApprovals() {
        LOG.debug("REST request to get pending biometric approvals");
        return biometricService.getPendingApprovals()
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * {@code GET /api/attendance/biometric/credentials} : Get all registered biometric credentials.
     */
    @GetMapping("/credentials")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public Mono<ResponseEntity<List<StaffBiometricDTO>>> getAllCredentials() {
        LOG.debug("REST request to get all biometric credentials");
        return biometricService.getAllCredentials()
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * {@code GET /api/attendance/biometric/user/:userId} : View biometric registration for a specific user.
     */
    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<List<StaffBiometricDTO>>> getCredentialsByUser(@PathVariable String userId) {
        LOG.debug("REST request to get biometric credentials for user: {}", userId);
        return biometricService.getCredentialsByUser(userId)
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * {@code PUT /api/attendance/biometric/:id/lock} : Admin locks and approves a staff member's fingerprint on an office device.
     */
    @PutMapping("/{id}/lock")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public Mono<ResponseEntity<StaffBiometricDTO>> lockCredential(@PathVariable Long id) {
        LOG.debug("REST request by Admin to lock biometric credential ID: {}", id);
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("admin")
            .flatMap(adminLogin -> biometricService.lockAndApprove(id, adminLogin))
            .map(ResponseEntity::ok);
    }

    /**
     * {@code PUT /api/attendance/biometric/:id/revoke} : Admin revokes an approved fingerprint credential.
     */
    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public Mono<ResponseEntity<StaffBiometricDTO>> revokeCredential(@PathVariable Long id) {
        LOG.debug("REST request by Admin to revoke biometric credential ID: {}", id);
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("admin")
            .flatMap(adminLogin -> biometricService.revokeCredential(id, adminLogin))
            .map(ResponseEntity::ok);
    }
}
