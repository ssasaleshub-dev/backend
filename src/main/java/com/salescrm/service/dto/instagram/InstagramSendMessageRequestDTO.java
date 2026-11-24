package com.salescrm.service.dto.instagram;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Request payload for sending an outbound Instagram Direct Message reply from SSA Saleshub.
 */
public class InstagramSendMessageRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "recipientId (IGSID) is required")
    private String recipientId;

    @NotBlank(message = "message text is required")
    private String message;

    private Long leadId;

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }
}
