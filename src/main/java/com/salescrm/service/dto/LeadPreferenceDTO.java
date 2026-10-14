package com.salescrm.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.salescrm.domain.LeadPreference} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LeadPreferenceDTO implements Serializable {

    private Long id;

    private String mattressType;

    private String mattressSize;

    private String budgetRange;

    private String purchaseTimeline;

    private String visaType;
    private String age;
    private String educationalQualification;
    private String areYouInterested;
    private String areYouWorking;
    private String leadConfirm;
    private String assignedTo;

    private Instant createdAt;

    private LeadDTO lead;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getLeadConfirm() {
        return leadConfirm;
    }

    public void setLeadConfirm(String leadConfirm) {
        this.leadConfirm = leadConfirm;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public LeadDTO getLead() {
        return lead;
    }

    public void setLead(LeadDTO lead) {
        this.lead = lead;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LeadPreferenceDTO)) {
            return false;
        }

        LeadPreferenceDTO leadPreferenceDTO = (LeadPreferenceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, leadPreferenceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LeadPreferenceDTO{" +
            "id=" + getId() +
            ", mattressType='" + getMattressType() + "'" +
            ", mattressSize='" + getMattressSize() + "'" +
            ", budgetRange='" + getBudgetRange() + "'" +
            ", purchaseTimeline='" + getPurchaseTimeline() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", lead=" + getLead() +
            "}";
    }
}
