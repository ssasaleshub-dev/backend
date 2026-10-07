package com.salescrm.domain;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Customer.
 */
@Table("customer")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Customer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("full_name")
    private String fullName;

    @NotNull(message = "must not be null")
    @Column("phone_number")
    private String phoneNumber;

    @Column("raw_phone_number")
    private String rawPhoneNumber;

    @Column("location")
    private String location;

    @Column("created_at")
    private Instant createdAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Customer id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return this.fullName;
    }

    public Customer fullName(String fullName) {
        this.setFullName(fullName);
        return this;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public Customer phoneNumber(String phoneNumber) {
        this.setPhoneNumber(phoneNumber);
        return this;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRawPhoneNumber() {
        return this.rawPhoneNumber;
    }

    public Customer rawPhoneNumber(String rawPhoneNumber) {
        this.setRawPhoneNumber(rawPhoneNumber);
        return this;
    }

    public void setRawPhoneNumber(String rawPhoneNumber) {
        this.rawPhoneNumber = rawPhoneNumber;
    }

    public String getLocation() {
        return this.location;
    }

    public Customer location(String location) {
        this.setLocation(location);
        return this;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Customer createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer)) {
            return false;
        }
        return getId() != null && getId().equals(((Customer) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Customer{" +
            "id=" + getId() +
            ", fullName='" + getFullName() + "'" +
            ", phoneNumber='" + getPhoneNumber() + "'" +
            ", rawPhoneNumber='" + getRawPhoneNumber() + "'" +
            ", location='" + getLocation() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
