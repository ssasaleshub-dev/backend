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
