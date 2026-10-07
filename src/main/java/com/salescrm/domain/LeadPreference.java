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
