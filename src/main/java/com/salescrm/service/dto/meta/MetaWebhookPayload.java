package com.salescrm.service.dto.meta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

/**
 * DTO representing Meta Webhook payload for LeadGen events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaWebhookPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("object")
    private String object;

    @JsonProperty("entry")
    private List<Entry> entry;

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
        this.entry = entry;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements Serializable {

        @JsonProperty("id")
        private String id;

        @JsonProperty("time")
        private Long time;

        @JsonProperty("changes")
        private List<Change> changes;

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
            this.changes = changes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change implements Serializable {

        @JsonProperty("field")
        private String field;

        @JsonProperty("value")
        private Value value;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public Value getValue() {
            return value;
        }

        public void setValue(Value value) {
            this.value = value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value implements Serializable {

        @JsonProperty("leadgen_id")
        private String leadgenId;

        @JsonProperty("page_id")
        private String pageId;

        @JsonProperty("form_id")
        private String formId;

        @JsonProperty("ad_id")
        private String adId;

        @JsonProperty("adgroup_id")
        private String adgroupId;

        @JsonProperty("created_time")
        private Long createdTime;

        public String getLeadgenId() {
            return leadgenId;
        }

        public void setLeadgenId(String leadgenId) {
            this.leadgenId = leadgenId;
        }

        public String getPageId() {
            return pageId;
        }

        public void setPageId(String pageId) {
            this.pageId = pageId;
        }

        public String getFormId() {
            return formId;
        }

        public void setFormId(String formId) {
            this.formId = formId;
        }

        public String getAdId() {
            return adId;
        }

        public void setAdId(String adId) {
            this.adId = adId;
        }

        public String getAdgroupId() {
            return adgroupId;
        }

        public void setAdgroupId(String adgroupId) {
            this.adgroupId = adgroupId;
        }

        public Long getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(Long createdTime) {
            this.createdTime = createdTime;
        }
    }
}

