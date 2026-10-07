package com.salescrm.service.dto;

import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import java.io.Serializable;
import java.time.Instant;

/**
 * Composite detailed DTO providing complete marketing and customer attribution
 * for front-end dashboards and rich CRM views.
 */
public class LeadDetailedDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String externalId;
    private Instant createdTime;
    private Boolean isOrganic;
    private PlatformType platform;
    private LeadStatus status;
    private String notes;
    private Instant createdAt;

    // Resolved Customer details
    private Long customerId;
    private String customerName;
    private String phoneNumber;
    private String location;

    // Resolved Marketing Attribution
    private Long adId;
    private String adName;
    private String adExternalId;

    private Long adSetId;
    private String adSetName;

    private Long campaignId;
    private String campaignName;
    private String campaignExternalId;

    private Long leadFormId;
    private String leadFormName;

    // Resolved Product / Survey Requirements
    private Long preferenceId;
    private String mattressType;
    private String mattressSize;
    private String budgetRange;
    private String purchaseTimeline;

    public LeadDetailedDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    public Boolean getIsOrganic() {
        return isOrganic;
    }

    public void setIsOrganic(Boolean organic) {
        isOrganic = organic;
    }

    public PlatformType getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformType platform) {
        this.platform = platform;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getAdId() {
        return adId;
    }

    public void setAdId(Long adId) {
        this.adId = adId;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public String getAdExternalId() {
        return adExternalId;
    }

    public void setAdExternalId(String adExternalId) {
        this.adExternalId = adExternalId;
    }

    public Long getAdSetId() {
        return adSetId;
    }

    public void setAdSetId(Long adSetId) {
        this.adSetId = adSetId;
    }

    public String getAdSetName() {
        return adSetName;
    }

    public void setAdSetName(String adSetName) {
        this.adSetName = adSetName;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getCampaignExternalId() {
        return campaignExternalId;
    }

    public void setCampaignExternalId(String campaignExternalId) {
        this.campaignExternalId = campaignExternalId;
    }

    public Long getLeadFormId() {
        return leadFormId;
    }

    public void setLeadFormId(Long leadFormId) {
        this.leadFormId = leadFormId;
    }

    public String getLeadFormName() {
        return leadFormName;
    }

    public void setLeadFormName(String leadFormName) {
        this.leadFormName = leadFormName;
    }

    public Long getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(Long preferenceId) {
        this.preferenceId = preferenceId;
    }

    public String getMattressType() {
        return mattressType;
    }

    public void setMattressType(String mattressType) {
        this.mattressType = mattressType;
    }

    public String getMattressSize() {
        return mattressSize;
    }

    public void setMattressSize(String mattressSize) {
        this.mattressSize = mattressSize;
    }

    public String getBudgetRange() {
        return budgetRange;
    }

    public void setBudgetRange(String budgetRange) {
        this.budgetRange = budgetRange;
    }

    public String getPurchaseTimeline() {
        return purchaseTimeline;
    }

    public void setPurchaseTimeline(String purchaseTimeline) {
        this.purchaseTimeline = purchaseTimeline;
    }
}
