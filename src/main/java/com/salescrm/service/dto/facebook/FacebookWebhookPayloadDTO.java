package com.salescrm.service.dto.facebook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing Meta Webhook notification event payloads for "leadgen".
 * Typically delivered with header: X-Hub-Signature-256
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookWebhookPayloadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("object")
    private String object;

    @JsonProperty("entry")
    private List<Entry> entry = new ArrayList<>();

    public FacebookWebhookPayloadDTO() {}

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public List<Entry> getEntry() {
        return entry;
    }

    public void setEntry(List<Entry> entry) {
        this.entry = entry != null ? entry : new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("id")
        private String id; // Facebook Page ID

        @JsonProperty("time")
        private Long time;

        @JsonProperty("changes")
        private List<Change> changes = new ArrayList<>();

        public Entry() {}

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Long getTime() {
            return time;
        }

        public void setTime(Long time) {
            this.time = time;
        }

        public List<Change> getChanges() {
            return changes;
        }

        public void setChanges(List<Change> changes) {
            this.changes = changes != null ? changes : new ArrayList<>();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("field")
        private String field; // Expected: "leadgen"

        @JsonProperty("value")
        private LeadgenValue value;

        public Change() {}

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public LeadgenValue getValue() {
            return value;
        }

        public void setValue(LeadgenValue value) {
            this.value = value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeadgenValue implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("ad_id")
        private String adId;

        @JsonProperty("form_id")
        private String formId;

        @JsonProperty("leadgen_id")
        private String leadgenId;

        @JsonProperty("created_time")
        private Long createdTime;

        @JsonProperty("page_id")
        private String pageId;

        @JsonProperty("adgroup_id")
        private String adgroupId;

        public LeadgenValue() {}

        public String getAdId() {
            return adId;
        }

        public void setAdId(String adId) {
            this.adId = adId;
        }

        public String getFormId() {
            return formId;
        }

        public void setFormId(String formId) {
            this.formId = formId;
        }

        public String getLeadgenId() {
            return leadgenId;
        }

        public void setLeadgenId(String leadgenId) {
            this.leadgenId = leadgenId;
        }

        public Long getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(Long createdTime) {
            this.createdTime = createdTime;
        }

        public String getPageId() {
            return pageId;
        }

        public void setPageId(String pageId) {
            this.pageId = pageId;
        }

        public String getAdgroupId() {
            return adgroupId;
        }

        public void setAdgroupId(String adgroupId) {
            this.adgroupId = adgroupId;
        }
    }

    @Override
    public String toString() {
        return "FacebookWebhookPayloadDTO{" +
            "object='" + object + '\'' +
            ", entryCount=" + (entry != null ? entry.size() : 0) +
            '}';
    }
}
