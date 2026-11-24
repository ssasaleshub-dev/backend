package com.salescrm.service.dto.instagram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Strongly-typed DTO for Meta's Instagram Messaging Webhook notifications.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstagramWebhookPayloadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("object")
    private String object; // "instagram"

    @JsonProperty("entry")
    private List<Entry> entry = new ArrayList<>();

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
        private String id; // Instagram Business Account ID

        @JsonProperty("time")
        private Long time;

        @JsonProperty("messaging")
        private List<Messaging> messaging = new ArrayList<>();

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

        public List<Messaging> getMessaging() {
            return messaging;
        }

        public void setMessaging(List<Messaging> messaging) {
            this.messaging = messaging != null ? messaging : new ArrayList<>();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Messaging implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("sender")
        private UserRef sender;

        @JsonProperty("recipient")
        private UserRef recipient;

        @JsonProperty("timestamp")
        private Long timestamp;

        @JsonProperty("message")
        private MessageData message;

        public UserRef getSender() {
            return sender;
        }

        public void setSender(UserRef sender) {
            this.sender = sender;
        }

        public UserRef getRecipient() {
            return recipient;
        }

        public void setRecipient(UserRef recipient) {
            this.recipient = recipient;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public MessageData getMessage() {
            return message;
        }

        public void setMessage(MessageData message) {
            this.message = message;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserRef implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("id")
        private String id; // IGSID (Instagram Scoped ID)

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("mid")
        private String mid;

        @JsonProperty("text")
        private String text;

        @JsonProperty("is_echo")
        private Boolean isEcho;

        @JsonProperty("attachments")
        private List<Attachment> attachments = new ArrayList<>();

        public String getMid() {
            return mid;
        }

        public void setMid(String mid) {
            this.mid = mid;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Boolean getIsEcho() {
            return isEcho;
        }

        public void setIsEcho(Boolean isEcho) {
            this.isEcho = isEcho;
        }

        public List<Attachment> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<Attachment> attachments) {
            this.attachments = attachments != null ? attachments : new ArrayList<>();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attachment implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("type")
        private String type;

        @JsonProperty("payload")
        private Payload payload;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Payload getPayload() {
            return payload;
        }

        public void setPayload(Payload payload) {
            this.payload = payload;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("url")
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
