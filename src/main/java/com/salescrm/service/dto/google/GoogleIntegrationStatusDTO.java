package com.salescrm.service.dto.google;

import java.io.Serializable;
import java.time.Instant;

public class GoogleIntegrationStatusDTO implements Serializable {

    private boolean connected;
    private String googleEmail;
    private Instant connectedAt;
    private Instant tokenExpiry;

    public GoogleIntegrationStatusDTO() {}

    public GoogleIntegrationStatusDTO(boolean connected, String googleEmail, Instant connectedAt, Instant tokenExpiry) {
        this.connected = connected;
        this.googleEmail = googleEmail;
        this.connectedAt = connectedAt;
        this.tokenExpiry = tokenExpiry;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getGoogleEmail() {
        return googleEmail;
    }

    public void setGoogleEmail(String googleEmail) {
        this.googleEmail = googleEmail;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Instant getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(Instant tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }
}

