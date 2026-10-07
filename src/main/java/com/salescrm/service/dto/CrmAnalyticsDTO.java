package com.salescrm.service.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Real-time analytics aggregation DTO for CRM executive and marketing dashboard.
 */
public class CrmAnalyticsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long totalLeads;
    private long totalCustomers;
    private long totalCampaigns;
    private long totalAds;

    private Map<String, Long> leadsByStatus = new HashMap<>();
    private Map<String, Long> leadsByPlatform = new HashMap<>();
    private Map<String, Long> leadsByLocation = new HashMap<>();
    private Map<String, Long> leadsByMattressSize = new HashMap<>();
    private Map<String, Long> leadsByBudget = new HashMap<>();

    public CrmAnalyticsDTO() {}

    public long getTotalLeads() {
        return totalLeads;
    }

    public void setTotalLeads(long totalLeads) {
        this.totalLeads = totalLeads;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalCampaigns() {
        return totalCampaigns;
    }

    public void setTotalCampaigns(long totalCampaigns) {
        this.totalCampaigns = totalCampaigns;
    }

    public long getTotalAds() {
        return totalAds;
    }

    public void setTotalAds(long totalAds) {
        this.totalAds = totalAds;
    }

    public Map<String, Long> getLeadsByStatus() {
        return leadsByStatus;
    }

    public void setLeadsByStatus(Map<String, Long> leadsByStatus) {
        this.leadsByStatus = leadsByStatus;
    }

    public Map<String, Long> getLeadsByPlatform() {
        return leadsByPlatform;
    }

    public void setLeadsByPlatform(Map<String, Long> leadsByPlatform) {
        this.leadsByPlatform = leadsByPlatform;
    }

    public Map<String, Long> getLeadsByLocation() {
        return leadsByLocation;
    }

    public void setLeadsByLocation(Map<String, Long> leadsByLocation) {
        this.leadsByLocation = leadsByLocation;
    }

    public Map<String, Long> getLeadsByMattressSize() {
        return leadsByMattressSize;
    }

    public void setLeadsByMattressSize(Map<String, Long> leadsByMattressSize) {
        this.leadsByMattressSize = leadsByMattressSize;
    }

    public Map<String, Long> getLeadsByBudget() {
        return leadsByBudget;
    }

    public void setLeadsByBudget(Map<String, Long> leadsByBudget) {
        this.leadsByBudget = leadsByBudget;
    }
}
