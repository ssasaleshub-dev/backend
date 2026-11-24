package com.salescrm.service.dto.instagram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Response payload returned after attempting to send an Instagram Direct Message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstagramSendMessageResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("recipient_id")
    private String recipientId;

    @JsonProperty("message_id")
    private String messageId;

    private boolean success = true;

    private String errorMessage;

    public InstagramSendMessageResponseDTO() {}

    public InstagramSendMessageResponseDTO(String recipientId, String messageId) {
        this.recipientId = recipientId;
        this.messageId = messageId;
        this.success = true;
    }

    public static InstagramSendMessageResponseDTO error(String errorMessage) {
        InstagramSendMessageResponseDTO res = new InstagramSendMessageResponseDTO();
        res.setSuccess(false);
        res.setErrorMessage(errorMessage);
        return res;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
