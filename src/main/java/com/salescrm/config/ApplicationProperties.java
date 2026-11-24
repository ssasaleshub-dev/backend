package com.salescrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Sales CRM.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = true)
public class ApplicationProperties {

    private FacebookProperties facebook = new FacebookProperties();
    private GoogleProperties google = new GoogleProperties();

    public FacebookProperties getFacebook() {
        return facebook;
    }

    public void setFacebook(FacebookProperties facebook) {
        this.facebook = facebook;
    }

    public GoogleProperties getGoogle() {
        return google;
    }

    public void setGoogle(GoogleProperties google) {
        this.google = google;
    }

    public static class GoogleProperties {

        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "https://ssasaleshub.cloud/api/integrations/google/callback";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}
