package com.salescrm.domain;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Stores OAuth 2.0 authorization tokens and profile for a user's connected Google account.
 */
@Table("user_google_integrations")
public class UserGoogleIntegration implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("user_id")
    private String userId;

    @Column("google_email")
    private String googleEmail;

    @Column("access_token")
    private String accessToken;

    @Column("refresh_token")
    private String refreshToken;

    @Column("token_expiry")
    private Instant tokenExpiry;

    @Column("scopes")
    private String scopes;

    @Column("is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Column("updated_at")
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGoogleEmail() {
        return googleEmail;
    }

    public void setGoogleEmail(String googleEmail) {
        this.googleEmail = googleEmail;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(Instant tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserGoogleIntegration that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId);
    }

    @Override
    public String toString() {
        return "UserGoogleIntegration{" +
            "id=" + id +
            ", userId='" + userId + '\'' +
            ", googleEmail='" + googleEmail + '\'' +
            ", isActive=" + isActive +
            ", tokenExpiry=" + tokenExpiry +
            '}';
    }
}

