package com.salescrm.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A LeadPreference.
 */
@Table("lead_preference")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LeadPreference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @Column("mattress_type")
    private String mattressType;

    @Column("mattress_size")
    private String mattressSize;

    @Column("budget_range")
    private String budgetRange;

    @Column("purchase_timeline")
    private String purchaseTimeline;

    @Column("visa_type")
    private String visaType;

    @Column("age")
    private String age;

    @Column("educational_qualification")
    private String educationalQualification;

    @Column("are_you_interested")
    private String areYouInterested;

    @Column("are_you_working")
    private String areYouWorking;

    @Column("lead_confirm")
    private String leadConfirm;

    @Column("assigned_to")
    private String assignedTo;

    @Column("created_at")
    private Instant createdAt;

    @org.springframework.data.annotation.Transient
    private Lead lead;

    @Column("lead_id")
    private Long leadId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public LeadPreference id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMattressType() {
        return this.mattressType;
    }

    public LeadPreference mattressType(String mattressType) {
        this.setMattressType(mattressType);
        return this;
    }

    public void setMattressType(String mattressType) {
        this.mattressType = mattressType;
    }

    public String getMattressSize() {
        return this.mattressSize;
    }

    public LeadPreference mattressSize(String mattressSize) {
        this.setMattressSize(mattressSize);
        return this;
    }

    public void setMattressSize(String mattressSize) {
        this.mattressSize = mattressSize;
    }

    public String getBudgetRange() {
        return this.budgetRange;
    }

    public LeadPreference budgetRange(String budgetRange) {
        this.setBudgetRange(budgetRange);
        return this;
    }

    public void setBudgetRange(String budgetRange) {
        this.budgetRange = budgetRange;
    }

    public String getPurchaseTimeline() {
        return this.purchaseTimeline;
    }

    public LeadPreference purchaseTimeline(String purchaseTimeline) {
        this.setPurchaseTimeline(purchaseTimeline);
        return this;
    }

    public void setPurchaseTimeline(String purchaseTimeline) {
        this.purchaseTimeline = purchaseTimeline;
    }

    public String getVisaType() {
        return this.visaType;
    }

    public LeadPreference visaType(String visaType) {
        this.setVisaType(visaType);
        return this;
    }

    public void setVisaType(String visaType) {
        this.visaType = visaType;
    }

    public String getAge() {
        return this.age;
    }

    public LeadPreference age(String age) {
        this.setAge(age);
        return this;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getEducationalQualification() {
        return this.educationalQualification;
    }

    public LeadPreference educationalQualification(String educationalQualification) {
        this.setEducationalQualification(educationalQualification);
        return this;
    }

    public void setEducationalQualification(String educationalQualification) {
        this.educationalQualification = educationalQualification;
    }

    public String getAreYouInterested() {
        return this.areYouInterested;
    }

    public LeadPreference areYouInterested(String areYouInterested) {
        this.setAreYouInterested(areYouInterested);
        return this;
    }

    public void setAreYouInterested(String areYouInterested) {
        this.areYouInterested = areYouInterested;
    }

    public String getAreYouWorking() {
        return this.areYouWorking;
    }

    public LeadPreference areYouWorking(String areYouWorking) {
        this.setAreYouWorking(areYouWorking);
        return this;
    }

    public void setAreYouWorking(String areYouWorking) {
        this.areYouWorking = areYouWorking;
    }

    public String getLeadConfirm() {
        return this.leadConfirm;
    }

    public LeadPreference leadConfirm(String leadConfirm) {
        this.setLeadConfirm(leadConfirm);
        return this;
    }

    public void setLeadConfirm(String leadConfirm) {
        this.leadConfirm = leadConfirm;
    }

    public String getAssignedTo() {
        return this.assignedTo;
    }

    public LeadPreference assignedTo(String assignedTo) {
        this.setAssignedTo(assignedTo);
        return this;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public LeadPreference createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Lead getLead() {
        return this.lead;
    }

    public void setLead(Lead lead) {
        this.lead = lead;
        this.leadId = lead != null ? lead.getId() : null;
    }

    public LeadPreference lead(Lead lead) {
        this.setLead(lead);
        return this;
    }

    public Long getLeadId() {
        return this.leadId;
    }

    public void setLeadId(Long lead) {
        this.leadId = lead;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LeadPreference)) {
            return false;
        }
        return getId() != null && getId().equals(((LeadPreference) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LeadPreference{" +
            "id=" + getId() +
            ", mattressType='" + getMattressType() + "'" +
            ", mattressSize='" + getMattressSize() + "'" +
            ", budgetRange='" + getBudgetRange() + "'" +
            ", purchaseTimeline='" + getPurchaseTimeline() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
