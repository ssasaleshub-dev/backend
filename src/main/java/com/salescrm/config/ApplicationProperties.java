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

    public FacebookProperties getFacebook() {
        return facebook;
    }

    public void setFacebook(FacebookProperties facebook) {
        this.facebook = facebook;
    }
}
