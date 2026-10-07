package com.salescrm.web.rest;

import com.salescrm.service.LeadAnalyticsService;
import com.salescrm.service.dto.CrmAnalyticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for retrieving CRM business analytics and campaign reporting.
 */
@RestController
@RequestMapping("/api/leads")
public class LeadAnalyticsResource {

    private static final Logger LOG = LoggerFactory.getLogger(LeadAnalyticsResource.class);

    private final LeadAnalyticsService analyticsService;

    public LeadAnalyticsResource(LeadAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * {@code GET /api/leads/analytics} : Get real-time aggregated CRM metrics.
     *
     * @return the {@link ResponseEntity} with body the analytics report
     */
    @GetMapping("/analytics")
    public Mono<ResponseEntity<CrmAnalyticsDTO>> getAnalytics() {
        LOG.debug("REST request to get CRM dashboard analytics");
        return analyticsService.getDashboardAnalytics()
            .map(ResponseEntity::ok);
    }
}
