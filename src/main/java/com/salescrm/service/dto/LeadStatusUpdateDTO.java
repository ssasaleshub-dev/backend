package com.salescrm.service.dto;

import com.salescrm.domain.enumeration.LeadStatus;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Request payload for updating a Lead's lifecycle status.
 */
public class LeadStatusUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Status cannot be null")
    private LeadStatus status;

    private String notes;

    public LeadStatusUpdateDTO() {}

    public LeadStatusUpdateDTO(LeadStatus status, String notes) {
        this.status = status;
        this.notes = notes;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
