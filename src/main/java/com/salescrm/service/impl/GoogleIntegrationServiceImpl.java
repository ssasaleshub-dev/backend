package com.salescrm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.config.ApplicationProperties;
import com.salescrm.domain.Lead;
import com.salescrm.domain.UserGoogleIntegration;
import com.salescrm.repository.LeadRepository;
import com.salescrm.repository.UserGoogleIntegrationRepository;
import com.salescrm.service.GoogleIntegrationService;
import com.salescrm.service.LeadImportService;
import com.salescrm.service.dto.google.GoogleIntegrationStatusDTO;
import com.salescrm.service.dto.google.GoogleTokenResponseDTO;
import com.salescrm.service.dto.google.GoogleUserInfoDTO;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
public class GoogleIntegrationServiceImpl implements GoogleIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleIntegrationServiceImpl.class);

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String GOOGLE_CALENDAR_EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events?conferenceDataVersion=1";
    private static final String GOOGLE_CALENDAR_PLAIN_EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String GOOGLE_SHEETS_BASE_URL = "https://sheets.googleapis.com/v4/spreadsheets";
    private static final String GOOGLE_DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";

    private final UserGoogleIntegrationRepository integrationRepository;
    private final ApplicationProperties applicationProperties;
    private final LeadImportService leadImportService;
    private final LeadRepository leadRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleIntegrationServiceImpl(
        UserGoogleIntegrationRepository integrationRepository,
        ApplicationProperties applicationProperties,
        LeadImportService leadImportService,
        LeadRepository leadRepository,
        WebClient.Builder webClientBuilder
    ) {
        this.integrationRepository = integrationRepository;
        this.applicationProperties = applicationProperties;
        this.leadImportService = leadImportService;
        this.leadRepository = leadRepository;
        this.webClient = webClientBuilder.build();
    }

    private ApplicationProperties.GoogleProperties getProps() {
        return applicationProperties.getGoogle();
    }

    @Override
    public String getAuthorizationUrl(String state) {
        ApplicationProperties.GoogleProperties props = getProps();
        String scopes = String.join(
            " ",
            "https://www.googleapis.com/auth/calendar.events",
            "https://www.googleapis.com/auth/spreadsheets.readonly",
            "https://www.googleapis.com/auth/drive.readonly",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"
        );

        return UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
            .queryParam("client_id", props.getClientId())
            .queryParam("redirect_uri", props.getRedirectUri())
            .queryParam("response_type", "code")
            .queryParam("scope", scopes)
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .queryParam("state", state != null ? state : "")
            .build()
            .toUriString();
    }

    @Override
    public Mono<UserGoogleIntegration> handleCallback(String code, String userId) {
        ApplicationProperties.GoogleProperties props = getProps();

        return webClient
            .post()
            .uri(GOOGLE_TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("client_id", props.getClientId())
                    .with("client_secret", props.getClientSecret())
                    .with("code", code)
                    .with("grant_type", "authorization_code")
                    .with("redirect_uri", props.getRedirectUri())
            )
            .retrieve()
            .bodyToMono(GoogleTokenResponseDTO.class)
            .flatMap(tokenResponse ->
                fetchUserInfo(tokenResponse.getAccessToken())
                    .flatMap(userInfo -> saveOrUpdateIntegration(userId, tokenResponse, userInfo.getEmail()))
            )
            .doOnSuccess(saved -> LOG.info("Successfully connected Google account for user: {}, email: {}", userId, saved.getGoogleEmail()))
            .doOnError(err -> LOG.error("Failed to complete Google OAuth exchange for user {}: {}", userId, err.getMessage()));
    }

    private Mono<GoogleUserInfoDTO> fetchUserInfo(String accessToken) {
        return webClient
            .get()
            .uri(GOOGLE_USERINFO_URL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(GoogleUserInfoDTO.class)
            .onErrorResume(err -> {
                LOG.warn("Failed to fetch Google userinfo: {}", err.getMessage());
                GoogleUserInfoDTO fallback = new GoogleUserInfoDTO();
                return Mono.just(fallback);
            });
    }

    private Mono<UserGoogleIntegration> saveOrUpdateIntegration(String userId, GoogleTokenResponseDTO tokenResponse, String email) {
        Instant expiry = Instant.now().plusSeconds(tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : 3600);

        return integrationRepository
            .findByUserId(userId)
            .defaultIfEmpty(new UserGoogleIntegration())
            .flatMap(integration -> {
                if (integration.getId() == null) {
                    integration.setUserId(userId);
                    integration.setCreatedAt(Instant.now());
                }
                if (email != null && !email.isBlank()) {
                    integration.setGoogleEmail(email);
                }
                integration.setAccessToken(tokenResponse.getAccessToken());
                if (tokenResponse.getRefreshToken() != null && !tokenResponse.getRefreshToken().isBlank()) {
                    integration.setRefreshToken(tokenResponse.getRefreshToken());
                }
                integration.setTokenExpiry(expiry);
                integration.setScopes(tokenResponse.getScope());
                integration.setIsActive(Boolean.TRUE);
                integration.setUpdatedAt(Instant.now());
                return integrationRepository.save(integration);
            });
    }

    @Override
    public Mono<GoogleIntegrationStatusDTO> getStatus(String userId) {
        return integrationRepository
            .findByUserIdAndIsActiveTrue(userId)
            .map(integration -> new GoogleIntegrationStatusDTO(true, integration.getGoogleEmail(), integration.getCreatedAt(), integration.getTokenExpiry()))
            .defaultIfEmpty(new GoogleIntegrationStatusDTO(false, null, null, null));
    }

    @Override
    public Mono<Void> disconnect(String userId) {
        return integrationRepository
            .findByUserId(userId)
            .flatMap(integration -> {
                integration.setIsActive(Boolean.FALSE);
                integration.setAccessToken(null);
                integration.setRefreshToken(null);
                integration.setUpdatedAt(Instant.now());
                return integrationRepository.save(integration);
            })
            .then()
            .doOnSuccess(v -> LOG.info("Disconnected Google integration for user: {}", userId));
    }

    @Override
    public Mono<String> getValidAccessToken(String userId) {
        return integrationRepository
            .findByUserIdAndIsActiveTrue(userId)
            .flatMap(integration -> {
                if (integration.getAccessToken() != null &&
                    integration.getTokenExpiry() != null &&
                    integration.getTokenExpiry().isAfter(Instant.now().plusSeconds(120))) {
                    return Mono.just(integration.getAccessToken());
                }

                if (integration.getRefreshToken() == null || integration.getRefreshToken().isBlank()) {
                    return Mono.error(new IllegalStateException("Google refresh token not found for user: " + userId));
                }

                return refreshAccessToken(integration);
            });
    }

    private Mono<String> refreshAccessToken(UserGoogleIntegration integration) {
        ApplicationProperties.GoogleProperties props = getProps();

        return webClient
            .post()
            .uri(GOOGLE_TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("client_id", props.getClientId())
                    .with("client_secret", props.getClientSecret())
                    .with("refresh_token", integration.getRefreshToken())
                    .with("grant_type", "refresh_token")
            )
            .retrieve()
            .bodyToMono(GoogleTokenResponseDTO.class)
            .flatMap(tokenResponse -> {
                integration.setAccessToken(tokenResponse.getAccessToken());
                if (tokenResponse.getRefreshToken() != null && !tokenResponse.getRefreshToken().isBlank()) {
                    integration.setRefreshToken(tokenResponse.getRefreshToken());
                }
                long expiresIn = tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : 3600;
                integration.setTokenExpiry(Instant.now().plusSeconds(expiresIn));
                integration.setUpdatedAt(Instant.now());
                return integrationRepository.save(integration).thenReturn(tokenResponse.getAccessToken());
            })
            .doOnError(err -> LOG.error("Failed to refresh Google access token for user {}: {}", integration.getUserId(), err.getMessage()));
    }

    @Override
    public Mono<String> createGoogleMeet(String userId, String title, String description, Instant startInstant, Instant endInstant) {
        return getValidAccessToken(userId)
            .flatMap(token -> {
                Map<String, Object> body = new HashMap<>();
                body.put("summary", title);
                body.put("description", (description != null && !description.isBlank()) ? description : "Meeting scheduled via SSA Saleshub");

                Map<String, String> start = Map.of("dateTime", startInstant.toString());
                Map<String, String> end = Map.of("dateTime", endInstant.toString());
                body.put("start", start);
                body.put("end", end);

                Map<String, Object> conferenceSolutionKey = Map.of("type", "hangoutsMeet");
                Map<String, Object> createRequest = Map.of(
                    "requestId", UUID.randomUUID().toString(),
                    "conferenceSolutionKey", conferenceSolutionKey
                );
                body.put("conferenceData", Map.of("createRequest", createRequest));

                return webClient
                    .post()
                    .uri(GOOGLE_CALENDAR_EVENTS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(responseBody -> {
                        try {
                            JsonNode json = objectMapper.readTree(responseBody);
                            if (json.has("hangoutLink")) {
                                String meetLink = json.get("hangoutLink").asText();
                                LOG.info("Created Google Meet link: {} for user: {}", meetLink, userId);
                                return Mono.just(meetLink);
                            }
                            if (json.has("conferenceData") && json.get("conferenceData").has("entryPoints")) {
                                for (JsonNode ep : json.get("conferenceData").get("entryPoints")) {
                                    if ("video".equalsIgnoreCase(ep.path("entryPointType").asText())) {
                                        return Mono.just(ep.path("uri").asText());
                                    }
                                }
                            }
                            return Mono.empty();
                        } catch (Exception e) {
                            LOG.error("Failed to parse Google Calendar event response: {}", e.getMessage());
                            return Mono.empty();
                        }
                    });
            })
            .onErrorResume(err -> {
                LOG.warn("Could not generate Google Meet for user {}: {}", userId, err.getMessage());
                return Mono.empty();
            });
    }

    @Override
    public Mono<String> createCalendarEvent(String userId, String title, String description, Instant startInstant, Instant endInstant) {
        return getValidAccessToken(userId)
            .flatMap(token -> {
                Map<String, Object> body = new HashMap<>();
                body.put("summary", (title != null && !title.isBlank()) ? title : "CRM Follow-up Reminder");
                body.put("description", (description != null && !description.isBlank()) ? description : "Scheduled via SSA SalesHub");
                body.put("start", Map.of("dateTime", startInstant.toString()));
                body.put("end", Map.of("dateTime", endInstant.toString()));

                return webClient
                    .post()
                    .uri(GOOGLE_CALENDAR_PLAIN_EVENTS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(responseBody -> {
                        try {
                            JsonNode json = objectMapper.readTree(responseBody);
                            String htmlLink = json.path("htmlLink").asText("");
                            LOG.info("Synced CRM reminder to Google Calendar for user {}: {}", userId, htmlLink);
                            return htmlLink;
                        } catch (Exception e) {
                            return "";
                        }
                    });
            })
            .onErrorResume(err -> {
                LOG.warn("Could not sync CRM reminder to Google Calendar for user {}: {}", userId, err.getMessage());
                return Mono.empty();
            });
    }

    @Override
    public Mono<String> exportLeadsToGoogleSheet(String userId, String sheetTitle, List<List<Object>> rows) {
        return getValidAccessToken(userId)
            .flatMap(token -> {
                Map<String, Object> createBody = Map.of("properties", Map.of("title", sheetTitle));

                return webClient
                    .post()
                    .uri(GOOGLE_SHEETS_BASE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(createResponse -> {
                        try {
                            JsonNode json = objectMapper.readTree(createResponse);
                            String spreadsheetId = json.path("spreadsheetId").asText();
                            String spreadsheetUrl = json.path("spreadsheetUrl").asText();

                            if (rows == null || rows.isEmpty()) {
                                return Mono.just(spreadsheetUrl);
                            }

                            String appendUrl = GOOGLE_SHEETS_BASE_URL + "/" + spreadsheetId + "/values/Sheet1!A1:append?valueInputOption=USER_ENTERED";
                            Map<String, Object> appendBody = Map.of("values", rows);

                            return webClient
                                .post()
                                .uri(appendUrl)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(appendBody)
                                .retrieve()
                                .bodyToMono(String.class)
                                .thenReturn(spreadsheetUrl);
                        } catch (Exception e) {
                            LOG.error("Failed to parse Google Sheets response: {}", e.getMessage());
                            return Mono.error(e);
                        }
                    });
            });
    }

    @Override
    public Mono<List<Map<String, String>>> listGoogleSheets(String userId) {
        return getValidAccessToken(userId)
            .flatMap(token -> {
                URI uri = UriComponentsBuilder.fromHttpUrl(GOOGLE_DRIVE_FILES_URL)
                    .queryParam("q", "mimeType='application/vnd.google-apps.spreadsheet' and trashed=false")
                    .queryParam("fields", "files(id,name,modifiedTime)")
                    .queryParam("orderBy", "modifiedTime desc")
                    .queryParam("pageSize", 50)
                    .encode()
                    .build()
                    .toUri();
                return webClient
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(status -> status.isError(), resp ->
                        resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(errBody -> {
                                LOG.error("Google Drive API error ({}): {}", resp.statusCode(), errBody);
                                return Mono.error(new RuntimeException("Google Drive API error (" + resp.statusCode() + "): " + errBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .map(body -> {
                        try {
                            JsonNode root = objectMapper.readTree(body);
                            List<Map<String, String>> list = new ArrayList<>();
                            if (root.has("files") && root.get("files").isArray()) {
                                for (JsonNode file : root.get("files")) {
                                    Map<String, String> item = new HashMap<>();
                                    item.put("id", file.path("id").asText());
                                    item.put("name", file.path("name").asText("Untitled Sheet"));
                                    item.put("modifiedTime", file.path("modifiedTime").asText(""));
                                    list.add(item);
                                }
                            }
                            return list;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse Google Drive sheets list: " + e.getMessage(), e);
                        }
                    });
            });
    }

    @Override
    public Mono<Map<String, Object>> previewGoogleSheet(String userId, String spreadsheetId) {
        return getValidAccessToken(userId)
            .flatMap(token -> {
                String metaUrl = GOOGLE_SHEETS_BASE_URL + "/" + spreadsheetId + "?fields=properties.title,sheets.properties.title";
                return webClient
                    .get()
                    .uri(metaUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(metaBody -> {
                        try {
                            JsonNode meta = objectMapper.readTree(metaBody);
                            String title = meta.path("properties").path("title").asText("Google Sheet");
                            String firstTab = "Sheet1";
                            if (meta.has("sheets") && meta.get("sheets").isArray() && meta.get("sheets").size() > 0) {
                                firstTab = meta.get("sheets").get(0).path("properties").path("title").asText("Sheet1");
                            }
                            String range = URLEncoder.encode(firstTab + "!A1:Z50", StandardCharsets.UTF_8);
                            String valuesUrl = GOOGLE_SHEETS_BASE_URL + "/" + spreadsheetId + "/values/" + range;
                            return webClient
                                .get()
                                .uri(URI.create(valuesUrl))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(String.class)
                                .map(valuesBody -> {
                                    try {
                                        JsonNode valJson = objectMapper.readTree(valuesBody);
                                        List<String> headers = new ArrayList<>();
                                        List<List<String>> rows = new ArrayList<>();
                                        JsonNode valuesNode = valJson.path("values");
                                        if (valuesNode.isArray() && valuesNode.size() > 0) {
                                            for (JsonNode cell : valuesNode.get(0)) {
                                                headers.add(cell.asText(""));
                                            }
                                            for (int i = 1; i < valuesNode.size(); i++) {
                                                List<String> row = new ArrayList<>();
                                                for (JsonNode cell : valuesNode.get(i)) {
                                                    row.add(cell.asText(""));
                                                }
                                                rows.add(row);
                                            }
                                        }
                                        Map<String, Object> result = new HashMap<>();
                                        result.put("spreadsheetId", spreadsheetId);
                                        result.put("title", title);
                                        result.put("headers", headers);
                                        result.put("rows", rows);
                                        result.put("totalRows", rows.size());
                                        return result;
                                    } catch (Exception e) {
                                        throw new RuntimeException("Failed to parse Google Sheet values: " + e.getMessage(), e);
                                    }
                                });
                        } catch (Exception e) {
                            return Mono.error(new RuntimeException("Failed to parse Google Sheet metadata: " + e.getMessage(), e));
                        }
                    });
            });
    }

    @Override
    public Mono<Map<String, Object>> importGoogleSheet(String userId, String spreadsheetId) {
        return getValidAccessToken(userId)
            .flatMap(token -> {
                String exportUrl = GOOGLE_DRIVE_FILES_URL + "/" + spreadsheetId + "/export?mimeType=text/csv";
                return webClient
                    .get()
                    .uri(URI.create(exportUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .flatMap(csvBytes ->
                        leadImportService.importFile(
                            "google-sheet-" + spreadsheetId + ".csv",
                            "text/csv",
                            new ByteArrayInputStream(csvBytes)
                        )
                    )
                    .flatMap(importResult ->
                        Flux.fromIterable(importResult.getImportedLeadExternalIds())
                            .flatMap(leadRepository::findByExternalId)
                            .map(Lead::getId)
                            .collectList()
                            .map(leadIds -> {
                                Map<String, Object> res = new HashMap<>();
                                res.put("importedCount", importResult.getSuccessfulRows());
                                res.put("leadIds", leadIds);
                                return res;
                            })
                    );
            });
    }
}
