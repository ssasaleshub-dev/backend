package com.salescrm.web.rest;

import com.salescrm.domain.UserGoogleIntegration;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.GoogleIntegrationService;
import com.salescrm.service.dto.google.GoogleIntegrationStatusDTO;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing Google Workspace integrations (Google Meet, Sheets, Drive).
 */
@RestController
@RequestMapping("/api/integrations/google")
public class GoogleIntegrationResource {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleIntegrationResource.class);

    private final GoogleIntegrationService googleIntegrationService;

    public GoogleIntegrationResource(GoogleIntegrationService googleIntegrationService) {
        this.googleIntegrationService = googleIntegrationService;
    }

    /**
     * {@code GET  /auth-url} : Returns the Google OAuth 2.0 authorization URL for frontend redirect.
     */
    @GetMapping("/auth-url")
    public Mono<ResponseEntity<Map<String, String>>> getAuthUrl(@RequestParam(required = false) String state) {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty(state != null ? state : "anonymous")
            .map(currentUser -> {
                String stateParam = (state != null && !state.isBlank()) ? state : currentUser;
                String authUrl = googleIntegrationService.getAuthorizationUrl(stateParam);
                return ResponseEntity.ok(Map.of("url", authUrl));
            });
    }

    /**
     * {@code GET  /callback} : Handles Google OAuth 2.0 browser redirect.
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> handleCallback(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error
    ) {
        if (error != null) {
            LOG.warn("Google OAuth callback returned error: {}", error);
            URI errorRedirect = URI.create("https://ssasaleshub.cloud/integrations?google_error=" + error);
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(errorRedirect).build());
        }

        if (code == null) {
            URI badRequestRedirect = URI.create("https://ssasaleshub.cloud/integrations?google_error=missing_code");
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(badRequestRedirect).build());
        }

        String targetUser = (state != null && !state.isBlank()) ? state : "admin";

        return googleIntegrationService
            .handleCallback(code, targetUser)
            .map(saved -> {
                URI successRedirect = URI.create("https://ssasaleshub.cloud/integrations?google_connected=true&email=" + (saved.getGoogleEmail() != null ? saved.getGoogleEmail() : ""));
                return ResponseEntity.status(HttpStatus.FOUND).location(successRedirect).<Void>build();
            })
            .onErrorResume(err -> {
                LOG.error("Failed handling Google callback: {}", err.getMessage());
                URI failureRedirect = URI.create("https://ssasaleshub.cloud/integrations?google_error=token_exchange_failed");
                return Mono.just(ResponseEntity.status(HttpStatus.FOUND).location(failureRedirect).build());
            });
    }

    /**
     * {@code POST  /exchange-code} : Manual code exchange endpoint for SPAs.
     */
    @PostMapping("/exchange-code")
    public Mono<ResponseEntity<GoogleIntegrationStatusDTO>> exchangeCode(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String user = payload.get("userId");

        if (code == null || code.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        Mono<String> resolveUser = (user != null && !user.isBlank())
            ? Mono.just(user)
            : SecurityUtils.getCurrentUserLogin().defaultIfEmpty("admin");

        return resolveUser
            .flatMap(userId -> googleIntegrationService.handleCallback(code, userId))
            .map(saved -> ResponseEntity.ok(new GoogleIntegrationStatusDTO(true, saved.getGoogleEmail(), saved.getCreatedAt(), saved.getTokenExpiry())));
    }

    /**
     * {@code GET  /status} : Checks whether the current user has connected Google.
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<GoogleIntegrationStatusDTO>> getStatus() {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("admin")
            .flatMap(googleIntegrationService::getStatus)
            .map(ResponseEntity::ok);
    }

    /**
     * {@code POST  /disconnect} : Disconnects Google integration for current user.
     */
    @PostMapping("/disconnect")
    public Mono<ResponseEntity<Void>> disconnect() {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("admin")
            .flatMap(googleIntegrationService::disconnect)
            .thenReturn(ResponseEntity.noContent().build());
    }

    /**
     * {@code POST  /meet} : Generates a Google Meet link for an event.
     */
    @PostMapping("/meet")
    public Mono<ResponseEntity<Map<String, String>>> createMeetLink(@RequestBody Map<String, Object> request) {
        String title = (String) request.getOrDefault("title", "Consultation Meeting");
        String description = (String) request.getOrDefault("description", "Scheduled via SSA Saleshub");

        Instant start = Instant.now().plusSeconds(600);
        Instant end = start.plusSeconds(1800);

        if (request.containsKey("startTime")) {
            try {
                start = Instant.parse(request.get("startTime").toString());
                end = request.containsKey("endTime") ? Instant.parse(request.get("endTime").toString()) : start.plusSeconds(1800);
            } catch (Exception ignored) {}
        }

        Instant finalStart = start;
        Instant finalEnd = end;

        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("admin")
            .flatMap(userId -> googleIntegrationService.createGoogleMeet(userId, title, description, finalStart, finalEnd))
            .map(meetUrl -> ResponseEntity.ok(Map.of("meetUrl", meetUrl)))
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(Map.of("error", "Google Calendar is not connected for this user")));
    }

    /**
     * {@code GET /sheets} : Lists the user's Google Sheets (import-only).
     */
    @GetMapping("/sheets")
    public Mono<ResponseEntity<?>> listGoogleSheets() {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("admin")
            .flatMap(googleIntegrationService::listGoogleSheets)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(Map.of("error", "Google Workspace integration is not connected for this user")))
            .onErrorResume(err -> {
                LOG.error("Failed to list Google Sheets: {}", err.getMessage());
                if (err instanceof WebClientResponseException wce) {
                    if (wce.getStatusCode() == HttpStatus.FORBIDDEN || wce.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error", "Insufficient Google Drive permissions. Please disconnect and reconnect your Google account in Settings to grant Drive access.",
                            "status", wce.getStatusCode().value()
                        )));
                    }
                    return Mono.just(ResponseEntity.status(wce.getStatusCode()).body(Map.of(
                        "error", "Google Drive API error: " + wce.getStatusCode(),
                        "details", wce.getResponseBodyAsString()
                    )));
                }
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to retrieve Google Sheets: " + err.getMessage()
                )));
            });
    }

    /**
     * {@code GET /sheets/{spreadsheetId}/preview} : Previews headers and rows of a Google Sheet.
     */
    @GetMapping("/sheets/{spreadsheetId}/preview")
    public Mono<ResponseEntity<?>> previewGoogleSheet(@PathVariable String spreadsheetId) {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("admin")
            .flatMap(userId -> googleIntegrationService.previewGoogleSheet(userId, spreadsheetId))
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(Map.of("error", "Google Workspace integration is not connected for this user")))
            .onErrorResume(err -> {
                LOG.error("Failed to preview Google Sheet {}: {}", spreadsheetId, err.getMessage());
                if (err instanceof WebClientResponseException wce) {
                    return Mono.just(ResponseEntity.status(wce.getStatusCode()).body(Map.of(
                        "error", "Google Sheets API error: " + wce.getStatusCode(),
                        "details", wce.getResponseBodyAsString()
                    )));
                }
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to preview sheet: " + err.getMessage()
                )));
            });
    }

    /**
     * {@code POST /sheets/{spreadsheetId}/import} : Imports leads from a Google Sheet.
     */
    @PostMapping("/sheets/{spreadsheetId}/import")
    public Mono<ResponseEntity<?>> importGoogleSheet(@PathVariable String spreadsheetId) {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("admin")
            .flatMap(userId -> googleIntegrationService.importGoogleSheet(userId, spreadsheetId))
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(Map.of("error", "Google Workspace integration is not connected for this user")))
            .onErrorResume(err -> {
                LOG.error("Failed to import Google Sheet {}: {}", spreadsheetId, err.getMessage());
                if (err instanceof WebClientResponseException wce) {
                    return Mono.just(ResponseEntity.status(wce.getStatusCode()).body(Map.of(
                        "error", "Google Sheets import error: " + wce.getStatusCode(),
                        "details", wce.getResponseBodyAsString()
                    )));
                }
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to import sheet: " + err.getMessage()
                )));
            });
    }
}
