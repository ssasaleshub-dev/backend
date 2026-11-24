package com.salescrm.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.domain.Customer;
import com.salescrm.domain.Lead;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import com.salescrm.repository.CustomerRepository;
import com.salescrm.repository.LeadRepository;
import com.salescrm.service.FirebasePushNotificationService;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Universal Inbound Webhook controller for ingesting external leads from
 * Wix Contact Forms, Jotform, Google Forms, UrbanPro, AAJJO, and Calendly.
 */
@RestController
@RequestMapping("/api/webhooks/inbound")
public class InboundWebhookResource {

    private static final Logger LOG = LoggerFactory.getLogger(InboundWebhookResource.class);

    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final FirebasePushNotificationService firebasePushNotificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InboundWebhookResource(
        CustomerRepository customerRepository,
        LeadRepository leadRepository,
        @Autowired(required = false) FirebasePushNotificationService firebasePushNotificationService
    ) {
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.firebasePushNotificationService = firebasePushNotificationService;
    }

    /**
     * {@code POST  /api/webhooks/inbound/{provider}} : Ingest external leads in real time.
     * Supported providers: wix, jotform, google-forms, urbanpro, aajjo, calendly.
     */
    @PostMapping("/{provider}")
    public Mono<ResponseEntity<Map<String, Object>>> ingestLead(
        @PathVariable String provider,
        @RequestBody String rawPayload
    ) {
        LOG.info("Received inbound webhook from provider: {}", provider);

        return Mono.fromCallable(() -> parsePayload(rawPayload))
            .flatMap(parsed -> saveCustomerAndLead(provider, parsed))
            .map(lead -> {
                if (firebasePushNotificationService != null) {
                    try {
                        firebasePushNotificationService.sendPushNotificationToAll(
                            "New " + capitalize(provider) + " Lead",
                            "Inquiry received via " + capitalize(provider) + " integration",
                            Map.of("leadId", String.valueOf(lead.getId()), "provider", provider)
                        );
                    } catch (Exception e) {
                        LOG.warn("Failed to dispatch push notification: {}", e.getMessage());
                    }
                }
                return ResponseEntity.ok(Map.<String, Object>of(
                    "status", "success",
                    "provider", provider,
                    "leadId", lead.getId()
                ));
            })
            .onErrorResume(err -> {
                LOG.error("Failed processing inbound webhook from {}: {}", provider, err.getMessage());
                return Mono.just(ResponseEntity.badRequest().body(Map.<String, Object>of(
                    "status", "error",
                    "provider", provider,
                    "message", err.getMessage()
                )));
            });
    }

    private ParsedInboundData parsePayload(String raw) {
        ParsedInboundData data = new ParsedInboundData();
        try {
            JsonNode root = objectMapper.readTree(raw);
            extractFieldsRecursively(root, data);
        } catch (Exception e) {
            LOG.warn("Failed to parse raw webhook payload as JSON: {}", e.getMessage());
        }

        if (data.name == null || data.name.isBlank()) {
            data.name = "Inbound Prospect";
        }
        if (data.phone == null || data.phone.isBlank()) {
            data.phone = "919999999999";
        }
        return data;
    }

    private void extractFieldsRecursively(JsonNode node, ParsedInboundData data) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey().toLowerCase();
                JsonNode value = entry.getValue();

                if (value.isValueNode()) {
                    String text = value.asText().trim();
                    if (text.isEmpty()) continue;

                    if (data.name == null && (key.contains("name") || key.contains("student") || key.contains("client") || key.equals("first_name"))) {
                        data.name = text;
                    } else if (data.phone == null && (key.contains("phone") || key.contains("mobile") || key.contains("contact") || key.contains("number"))) {
                        data.phone = text;
                    } else if (data.email == null && (key.contains("email") || key.contains("mail"))) {
                        data.email = text;
                    } else if (key.contains("message") || key.contains("note") || key.contains("course") || key.contains("inquiry") || key.contains("remark")) {
                        data.notes = (data.notes == null ? "" : data.notes + " | ") + entry.getKey() + ": " + text;
                    }
                } else if (value.isObject() || value.isArray()) {
                    extractFieldsRecursively(value, data);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                extractFieldsRecursively(item, data);
            }
        }
    }

    private Mono<Lead> saveCustomerAndLead(String provider, ParsedInboundData data) {
        String cleanPhone = data.phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() == 10) {
            cleanPhone = "91" + cleanPhone;
        }

        final String finalPhone = cleanPhone;

        return customerRepository
            .findByPhoneNumber(finalPhone)
            .switchIfEmpty(
                Mono.defer(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setFullName(data.name);
                    newCustomer.setPhoneNumber(finalPhone);
                    newCustomer.setRawPhoneNumber(data.phone);
                    newCustomer.setEmail(data.email);
                    newCustomer.setCreatedAt(Instant.now());
                    return customerRepository.save(newCustomer);
                })
            )
            .flatMap(customer -> {
                Lead lead = new Lead();
                lead.setCustomerId(customer.getId());
                lead.setExternalId("ext_" + provider + "_" + UUID.randomUUID().toString().substring(0, 8));
                lead.setPlatform(PlatformType.OTHER);
                lead.setStatus(LeadStatus.CREATED);
                lead.setIsOrganic(Boolean.TRUE);
                lead.setCreatedTime(Instant.now());
                lead.setCreatedAt(Instant.now());
                lead.setUpdatedAt(Instant.now());
                lead.setNotes("[" + capitalize(provider) + " Lead] " + (data.notes != null ? data.notes : ""));
                return leadRepository.save(lead);
            });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }

    private static class ParsedInboundData {
        String name;
        String phone;
        String email;
        String notes;
    }
}

