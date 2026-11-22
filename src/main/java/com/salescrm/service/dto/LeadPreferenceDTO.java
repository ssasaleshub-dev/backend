package com.salescrm.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.salescrm.domain.LeadPreference} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LeadPreferenceDTO implements Serializable {

    private Long id;

    private String budgetRange;

    private String purchaseTimeline;

    // Financial & Payment Aliases
    private String clientBudget;
    // "mattressType" / "mattressSize" are accepted so app versions released before the rename keep working
    @JsonAlias("mattressType")
    private String paymentStatus;

    @JsonAlias("mattressSize")
    private String paymentDetails;

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

    public String getBudgetRange() {
        return budgetRange != null ? budgetRange : clientBudget;
    }

    public void setBudgetRange(String budgetRange) {
        this.budgetRange = budgetRange;
        if (this.clientBudget == null) {
            this.clientBudget = budgetRange;
        }
    }

    public String getClientBudget() {
        return clientBudget != null ? clientBudget : budgetRange;
    }

    public void setClientBudget(String clientBudget) {
        this.clientBudget = clientBudget;
        if (clientBudget != null) {
            this.budgetRange = clientBudget;
        }
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(String paymentDetails) {
        this.paymentDetails = paymentDetails;
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
            ", paymentStatus='" + getPaymentStatus() + "'" +
            ", paymentDetails='" + getPaymentDetails() + "'" +
            ", budgetRange='" + getBudgetRange() + "'" +
            ", purchaseTimeline='" + getPurchaseTimeline() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", lead=" + getLead() +
            "}";
    }
}
