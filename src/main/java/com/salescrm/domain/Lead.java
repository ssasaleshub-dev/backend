package com.salescrm.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Lead.
 */
@Table("lead")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Lead implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("external_id")
    private String externalId;

    @NotNull(message = "must not be null")
    @Column("created_time")
    private Instant createdTime;

    @NotNull(message = "must not be null")
    @Column("is_organic")
    private Boolean isOrganic;

    @NotNull(message = "must not be null")
    @Column("platform")
    private PlatformType platform;

    @NotNull(message = "must not be null")
    @Column("status")
    private LeadStatus status;

    @Column("notes")
    private String notes;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @org.springframework.data.annotation.Transient
    private Customer customer;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "adSet" }, allowSetters = true)
    private Ad ad;

    @org.springframework.data.annotation.Transient
    private LeadForm leadForm;

    @org.springframework.data.annotation.Transient
    private LeadPreference leadPreference;

    @Column("customer_id")
    private Long customerId;

    @Column("ad_id")
    private Long adId;

    @Column("lead_form_id")
    private Long leadFormId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Lead id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public Lead externalId(String externalId) {
        this.setExternalId(externalId);
        return this;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Instant getCreatedTime() {
        return this.createdTime;
    }

    public Lead createdTime(Instant createdTime) {
        this.setCreatedTime(createdTime);
        return this;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    public Boolean getIsOrganic() {
        return this.isOrganic;
    }

    public Lead isOrganic(Boolean isOrganic) {
        this.setIsOrganic(isOrganic);
        return this;
    }

    public void setIsOrganic(Boolean isOrganic) {
        this.isOrganic = isOrganic;
    }

    public PlatformType getPlatform() {
        return this.platform;
    }

    public Lead platform(PlatformType platform) {
        this.setPlatform(platform);
        return this;
    }

    public void setPlatform(PlatformType platform) {
        this.platform = platform;
    }

    public LeadStatus getStatus() {
        return this.status;
    }

    public Lead status(LeadStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return this.notes;
    }

    public Lead notes(String notes) {
        this.setNotes(notes);
        return this;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Lead createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Lead updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Customer getCustomer() {
        return this.customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
        this.customerId = customer != null ? customer.getId() : null;
    }

    public Lead customer(Customer customer) {
        this.setCustomer(customer);
        return this;
    }

    public Ad getAd() {
        return this.ad;
    }

    public void setAd(Ad ad) {
        this.ad = ad;
        this.adId = ad != null ? ad.getId() : null;
    }

    public Lead ad(Ad ad) {
        this.setAd(ad);
        return this;
    }

    public LeadForm getLeadForm() {
        return this.leadForm;
    }

    public void setLeadForm(LeadForm leadForm) {
        this.leadForm = leadForm;
        this.leadFormId = leadForm != null ? leadForm.getId() : null;
    }

    public Lead leadForm(LeadForm leadForm) {
        this.setLeadForm(leadForm);
        return this;
    }

    public LeadPreference getLeadPreference() {
        return this.leadPreference;
    }

    public void setLeadPreference(LeadPreference leadPreference) {
        if (this.leadPreference != null) {
            this.leadPreference.setLead(null);
        }
        if (leadPreference != null) {
            leadPreference.setLead(this);
        }
        this.leadPreference = leadPreference;
    }

    public Lead leadPreference(LeadPreference leadPreference) {
        this.setLeadPreference(leadPreference);
        return this;
    }

    public Long getCustomerId() {
        return this.customerId;
    }

    public void setCustomerId(Long customer) {
        this.customerId = customer;
    }

    public Long getAdId() {
        return this.adId;
    }

    public void setAdId(Long ad) {
        this.adId = ad;
    }

    public Long getLeadFormId() {
        return this.leadFormId;
    }

    public void setLeadFormId(Long leadForm) {
        this.leadFormId = leadForm;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Lead)) {
            return false;
        }
        return getId() != null && getId().equals(((Lead) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Lead{" +
            "id=" + getId() +
            ", externalId='" + getExternalId() + "'" +
            ", createdTime='" + getCreatedTime() + "'" +
            ", isOrganic='" + getIsOrganic() + "'" +
            ", platform='" + getPlatform() + "'" +
            ", status='" + getStatus() + "'" +
            ", notes='" + getNotes() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
