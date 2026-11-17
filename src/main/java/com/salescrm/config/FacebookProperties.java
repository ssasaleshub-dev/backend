package com.salescrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties for Meta/Facebook Lead Ads integration.
 * Loaded from application.yml under prefix "application.facebook".
 */
@Configuration
@ConfigurationProperties(prefix = "application.facebook", ignoreUnknownFields = true)
public class FacebookProperties {

    /**
     * Meta Developer App ID.
     */
    private String appId;

    /**
     * Meta Developer App Secret, used for HMAC-SHA256 signature verification.
     */
    private String appSecret;

    /**
     * Secret verification token configured in Meta Developer Portal for webhook handshake.
     */
    private String verifyToken = "salescrm_leadgen_secret_token";

    /**
     * Long-lived Page Access Token with 'leads_retrieval' and 'pages_manage_ads' permissions.
     */
    private String pageAccessToken;

    /**
     * Meta Graph API version (e.g. "v21.0").
     */
    private String graphApiVersion = "v21.0";

    /**
     * Base URL for Meta Graph API.
     */
    private String graphApiBaseUrl = "https://graph.facebook.com";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getPageAccessToken() {
        return pageAccessToken;
    }

    public void setPageAccessToken(String pageAccessToken) {
        this.pageAccessToken = pageAccessToken;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public void setGraphApiVersion(String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
    }

    public String getGraphApiBaseUrl() {
        return graphApiBaseUrl;
    }

    public void setGraphApiBaseUrl(String graphApiBaseUrl) {
        this.graphApiBaseUrl = graphApiBaseUrl;
    }
}
