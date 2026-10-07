package com.salescrm.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.salescrm.domain.Ad} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AdDTO implements Serializable {

    private Long id;

    @NotNull(message = "must not be null")
    private String externalId;

    @NotNull(message = "must not be null")
    private String name;

    private String status;

    private Instant createdAt;

    private AdSetDTO adSet;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public AdSetDTO getAdSet() {
        return adSet;
    }

    public void setAdSet(AdSetDTO adSet) {
        this.adSet = adSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdDTO)) {
            return false;
        }

        AdDTO adDTO = (AdDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, adDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AdDTO{" +
            "id=" + getId() +
            ", externalId='" + getExternalId() + "'" +
            ", name='" + getName() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", adSet=" + getAdSet() +
            "}";
    }
}
