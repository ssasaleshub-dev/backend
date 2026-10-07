package com.salescrm.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Ad.
 */
@Table("ad")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Ad implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("external_id")
    private String externalId;

    @NotNull(message = "must not be null")
    @Column("name")
    private String name;

    @Column("status")
    private String status;

    @Column("created_at")
    private Instant createdAt;

    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "campaign" }, allowSetters = true)
    private AdSet adSet;

    @Column("ad_set_id")
    private Long adSetId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Ad id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public Ad externalId(String externalId) {
        this.setExternalId(externalId);
        return this;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return this.name;
    }

    public Ad name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return this.status;
    }

    public Ad status(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Ad createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public AdSet getAdSet() {
        return this.adSet;
    }

    public void setAdSet(AdSet adSet) {
        this.adSet = adSet;
        this.adSetId = adSet != null ? adSet.getId() : null;
    }

    public Ad adSet(AdSet adSet) {
        this.setAdSet(adSet);
        return this;
    }

    public Long getAdSetId() {
        return this.adSetId;
    }

    public void setAdSetId(Long adSet) {
        this.adSetId = adSet;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ad)) {
            return false;
        }
        return getId() != null && getId().equals(((Ad) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Ad{" +
            "id=" + getId() +
            ", externalId='" + getExternalId() + "'" +
            ", name='" + getName() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
