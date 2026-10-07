package com.salescrm.service;

import com.salescrm.service.dto.CrmAnalyticsDTO;
import reactor.core.publisher.Mono;

/**
 * Service interface for real-time CRM reporting and business intelligence metrics.
 */
public interface LeadAnalyticsService {

    /**
     * Aggregates and returns core marketing performance and lead status metrics.
     *
     * @return Mono of CrmAnalyticsDTO
     */
    Mono<CrmAnalyticsDTO> getDashboardAnalytics();
}
