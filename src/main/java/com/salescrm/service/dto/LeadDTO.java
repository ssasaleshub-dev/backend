package com.salescrm.service.dto;

import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.salescrm.domain.Lead} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LeadDTO implements Serializable {

    private Long id;

    @NotNull(message = "must not be null")
    private String externalId;

    @NotNull(message = "must not be null")
    private Instant createdTime;

    @NotNull(message = "must not be null")
    private Boolean isOrganic;

    @NotNull(message = "must not be null")
    private PlatformType platform;

    @NotNull(message = "must not be null")
    private LeadStatus status;

    private String notes;

    private Instant createdAt;

    private Instant updatedAt;

    private CustomerDTO customer;

    private AdDTO ad;

    private LeadFormDTO leadForm;

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

    public void setIsOrganic(Boolean isOrganic) {
        this.isOrganic = isOrganic;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

    public AdDTO getAd() {
        return ad;
    }

    public void setAd(AdDTO ad) {
        this.ad = ad;
    }

    public LeadFormDTO getLeadForm() {
        return leadForm;
    }

    public void setLeadForm(LeadFormDTO leadForm) {
        this.leadForm = leadForm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LeadDTO)) {
            return false;
        }

        LeadDTO leadDTO = (LeadDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, leadDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LeadDTO{" +
            "id=" + getId() +
            ", externalId='" + getExternalId() + "'" +
            ", createdTime='" + getCreatedTime() + "'" +
            ", isOrganic='" + getIsOrganic() + "'" +
            ", platform='" + getPlatform() + "'" +
            ", status='" + getStatus() + "'" +
            ", notes='" + getNotes() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", customer=" + getCustomer() +
            ", ad=" + getAd() +
            ", leadForm=" + getLeadForm() +
            "}";
    }
}
