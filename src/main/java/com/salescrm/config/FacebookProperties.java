package com.salescrm.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties for Meta/Facebook Lead Ads integration.
 * Supports primary account plus additional Meta accounts/pages under "application.facebook.accounts".
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
     * Secret verification token configured in Meta Developer Portal for webhook
     * handshake.
     */
    private String verifyToken = "salescrm_leadgen_secret_token";

    /**
     * Long-lived Page Access Token with 'leads_retrieval' and 'pages_manage_ads'
     * permissions.
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

    /**
     * Additional Meta developer accounts / Facebook Pages (e.g., separate marketing teams).
     */
    private List<MetaAccountConfig> accounts = new ArrayList<>();

    public List<MetaAccountConfig> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<MetaAccountConfig> accounts) {
        this.accounts = accounts != null ? accounts : new ArrayList<>();
    }

    /**
     * Resolves the matching MetaAccountConfig for a given Facebook Page ID, if configured.
     */
    public MetaAccountConfig findAccountByPageId(String pageId) {
        if (pageId == null || pageId.isBlank() || accounts == null) {
            return null;
        }
        String targetPageId = pageId.trim();
        for (MetaAccountConfig account : accounts) {
            if (account != null && account.matchesPageId(targetPageId)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Resolves the Page Access Token for a given Page ID, falling back to the primary pageAccessToken.
     */
    public String resolvePageAccessToken(String pageId) {
        MetaAccountConfig match = findAccountByPageId(pageId);
        if (match != null && match.getPageAccessToken() != null && !match.getPageAccessToken().isBlank()) {
            return match.getPageAccessToken().trim();
        }
        return pageAccessToken != null ? pageAccessToken.trim() : null;
    }

    public static class MetaAccountConfig {
        private String name;
        private String appId;
        private String appSecret;
        private String verifyToken;
        private String pageId;
        private List<String> pageIds = new ArrayList<>();
        private String pageAccessToken;
        private List<String> allowedCampaignIds = new ArrayList<>();
        private List<String> allowedFormIds = new ArrayList<>();

        public boolean matchesPageId(String targetPageId) {
            if (targetPageId == null || targetPageId.isBlank()) {
                return false;
            }
            if (pageId != null && !pageId.isBlank()) {
                for (String id : pageId.split(",")) {
                    if (targetPageId.equals(id.trim())) {
                        return true;
                    }
                }
            }
            if (pageIds != null) {
                for (String id : pageIds) {
                    if (id != null && targetPageId.equals(id.trim())) {
                        return true;
                    }
                }
            }
            return false;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

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

        public String getPageId() {
            return pageId;
        }

        public void setPageId(String pageId) {
            this.pageId = pageId;
        }

        public List<String> getPageIds() {
            return pageIds;
        }

        public void setPageIds(List<String> pageIds) {
            this.pageIds = pageIds != null ? pageIds : new ArrayList<>();
        }

        public String getPageAccessToken() {
            return pageAccessToken;
        }

        public void setPageAccessToken(String pageAccessToken) {
            this.pageAccessToken = pageAccessToken;
        }

        public List<String> getAllowedCampaignIds() {
            return allowedCampaignIds;
        }

        public void setAllowedCampaignIds(List<String> allowedCampaignIds) {
            this.allowedCampaignIds = allowedCampaignIds != null ? allowedCampaignIds : new ArrayList<>();
        }

        public List<String> getAllowedFormIds() {
            return allowedFormIds;
        }

        public void setAllowedFormIds(List<String> allowedFormIds) {
            this.allowedFormIds = allowedFormIds != null ? allowedFormIds : new ArrayList<>();
        }
    }

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
