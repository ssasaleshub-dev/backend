package com.salescrm.service;

import com.salescrm.domain.UserGoogleIntegration;
import com.salescrm.service.dto.google.GoogleIntegrationStatusDTO;
import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Service for managing Google Workspace integrations (Google Meet, Google Sheets, Google Drive).
 */
public interface GoogleIntegrationService {

    /**
     * Generates the OAuth 2.0 authorization URL to initiate the Google consent flow.
     */
    String getAuthorizationUrl(String state);

    /**
     * Exchanges the authorization code received from Google for access & refresh tokens
     * and persists the integration for the user.
     */
    Mono<UserGoogleIntegration> handleCallback(String code, String userId);

    /**
     * Gets the current Google connection status for a user.
     */
    Mono<GoogleIntegrationStatusDTO> getStatus(String userId);

    /**
     * Disconnects the user's Google account and clears tokens.
     */
    Mono<Void> disconnect(String userId);

    /**
     * Retrieves a valid access token for the user, refreshing it automatically if expired.
     */
    Mono<String> getValidAccessToken(String userId);

    /**
     * Creates a Google Calendar event with an auto-generated Google Meet video conference link.
     */
    Mono<String> createGoogleMeet(String userId, String title, String description, Instant startInstant, Instant endInstant);

    /**
     * Creates a new Google Sheet or appends rows to an existing spreadsheet.
     */
    Mono<String> exportLeadsToGoogleSheet(String userId, String sheetTitle, List<List<Object>> rows);
}
