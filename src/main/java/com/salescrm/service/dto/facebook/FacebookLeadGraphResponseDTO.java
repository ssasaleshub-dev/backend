package com.salescrm.service.dto.facebook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the response from Meta Graph API endpoint:
 * GET /{graph_version}/{leadgen_id}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookLeadGraphResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private String id;

    @JsonProperty("created_time")
    private String createdTime;

    @JsonProperty("ad_id")
    private String adId;

    @JsonProperty("ad_name")
    private String adName;

    @JsonProperty("adset_id")
    private String adsetId;

    @JsonProperty("adset_name")
    private String adsetName;

    @JsonProperty("campaign_id")
    private String campaignId;

    @JsonProperty("campaign_name")
    private String campaignName;

    @JsonProperty("form_id")
    private String formId;

    @JsonProperty("is_organic")
    private Boolean isOrganic;

    @JsonProperty("platform")
    private String platform;

    @JsonProperty("field_data")
    private List<FacebookFieldDataDTO> fieldData = new ArrayList<>();

    public FacebookLeadGraphResponseDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public String getAdsetId() {
        return adsetId;
    }

    public void setAdsetId(String adsetId) {
        this.adsetId = adsetId;
    }

    public String getAdsetName() {
        return adsetName;
    }

    public void setAdsetName(String adsetName) {
        this.adsetName = adsetName;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public Boolean getIsOrganic() {
        return isOrganic;
    }

    public void setIsOrganic(Boolean isOrganic) {
        this.isOrganic = isOrganic;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public List<FacebookFieldDataDTO> getFieldData() {
        return fieldData;
    }

    public void setFieldData(List<FacebookFieldDataDTO> fieldData) {
        this.fieldData = fieldData != null ? fieldData : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "FacebookLeadGraphResponseDTO{" +
            "id='" + id + '\'' +
            ", createdTime='" + createdTime + '\'' +
            ", adId='" + adId + '\'' +
            ", adName='" + adName + '\'' +
            ", adsetId='" + adsetId + '\'' +
            ", adsetName='" + adsetName + '\'' +
            ", campaignId='" + campaignId + '\'' +
            ", campaignName='" + campaignName + '\'' +
            ", formId='" + formId + '\'' +
            ", isOrganic=" + isOrganic +
            ", platform='" + platform + '\'' +
            ", fieldDataCount=" + (fieldData != null ? fieldData.size() : 0) +
            '}';
    }
}
