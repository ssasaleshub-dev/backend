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

    // Visa & Education details from Meta Ads Form
    private String visaType;
    private String age;
    private String educationalQualification;
    private String currentStatus;
    private String cgpaOrPercentage;
    private String areYouInterested;
    private String areYouWorking;
    private String city;
    private String leadConfirm;

    // Contact info
    @NotBlank(message = "Customer full name cannot be empty")
    private String fullName;

    private String email;

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

    public String getVisaType() {
        return visaType;
    }

    public void setVisaType(String visaType) {
        this.visaType = visaType;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getEducationalQualification() {
        return educationalQualification;
    }

    public void setEducationalQualification(String educationalQualification) {
        this.educationalQualification = educationalQualification;
    }

    public String getAreYouInterested() {
        return areYouInterested;
    }

    public void setAreYouInterested(String areYouInterested) {
        this.areYouInterested = areYouInterested;
    }

    public String getAreYouWorking() {
        return areYouWorking;
    }

    public void setAreYouWorking(String areYouWorking) {
        this.areYouWorking = areYouWorking;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLeadConfirm() {
        return leadConfirm;
    }

    public void setLeadConfirm(String leadConfirm) {
        this.leadConfirm = leadConfirm;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getCgpaOrPercentage() {
        return cgpaOrPercentage;
    }

    public void setCgpaOrPercentage(String cgpaOrPercentage) {
        this.cgpaOrPercentage = cgpaOrPercentage;
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
