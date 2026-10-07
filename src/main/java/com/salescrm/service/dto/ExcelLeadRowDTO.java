package com.salescrm.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Data Transfer Object representing a single un-normalized row from the raw Excel sheet.
 * Captures all columns provided in the marketing lead export.
 */
public class ExcelLeadRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int rowNumber;

    @NotBlank(message = "Lead ID cannot be empty")
    private String id;

    @NotBlank(message = "Created time cannot be empty")
    private String createdTime;

    private String adId;
    private String adName;
    private String adsetId;
    private String adsetName;
    private String campaignId;
    private String campaignName;
    private String formId;
    private String formName;

    private String isOrganic;
    private String platform;

    // Survey / Questionnaire questions from Instant Form
    private String mattressType;
    private String mattressSize;
    private String preferredBudget;
    private String planningToBuy;
    private String location;

    // Contact info
    @NotBlank(message = "Customer full name cannot be empty")
    private String fullName;

    @NotBlank(message = "Phone number cannot be empty")
    private String phoneNumber;

    private String leadStatus;

    public ExcelLeadRowDTO() {}

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

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

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getIsOrganic() {
        return isOrganic;
    }

    public void setIsOrganic(String isOrganic) {
        this.isOrganic = isOrganic;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public String getPreferredBudget() {
        return preferredBudget;
    }

    public void setPreferredBudget(String preferredBudget) {
        this.preferredBudget = preferredBudget;
    }

    public String getPlanningToBuy() {
        return planningToBuy;
    }

    public void setPlanningToBuy(String planningToBuy) {
        this.planningToBuy = planningToBuy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLeadStatus() {
        return leadStatus;
    }

    public void setLeadStatus(String leadStatus) {
        this.leadStatus = leadStatus;
    }

    @Override
    public String toString() {
        return "ExcelLeadRowDTO{" +
            "rowNumber=" + rowNumber +
            ", id='" + id + '\'' +
            ", fullName='" + fullName + '\'' +
            ", phoneNumber='" + phoneNumber + '\'' +
            ", platform='" + platform + '\'' +
            ", leadStatus='" + leadStatus + '\'' +
            '}';
    }
}
