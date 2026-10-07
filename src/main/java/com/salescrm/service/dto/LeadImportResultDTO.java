package com.salescrm.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO containing statistics and outcomes of an Excel/CSV ingestion job.
 */
public class LeadImportResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;
    private int totalRowsProcessed;
    private int successfulRows;
    private int failedRows;
    private long executionTimeMs;
    private List<String> importedLeadExternalIds = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public LeadImportResultDTO() {}

    public LeadImportResultDTO(String fileName) {
        this.fileName = fileName;
    }

    public static LeadImportResultDTO create(String fileName) {
        return new LeadImportResultDTO(fileName);
    }

    public void addSuccess(String leadExternalId) {
        this.successfulRows++;
        this.importedLeadExternalIds.add(leadExternalId);
    }

    public void addError(String error) {
        this.failedRows++;
        this.errors.add(error);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getTotalRowsProcessed() {
        return totalRowsProcessed;
    }

    public void setTotalRowsProcessed(int totalRowsProcessed) {
        this.totalRowsProcessed = totalRowsProcessed;
    }

    public int getSuccessfulRows() {
        return successfulRows;
    }

    public void setSuccessfulRows(int successfulRows) {
        this.successfulRows = successfulRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int failedRows) {
        this.failedRows = failedRows;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public List<String> getImportedLeadExternalIds() {
        return importedLeadExternalIds;
    }

    public void setImportedLeadExternalIds(List<String> importedLeadExternalIds) {
        this.importedLeadExternalIds = importedLeadExternalIds;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "LeadImportResultDTO{" +
            "fileName='" + fileName + '\'' +
            ", totalRowsProcessed=" + totalRowsProcessed +
            ", successfulRows=" + successfulRows +
            ", failedRows=" + failedRows +
            ", executionTimeMs=" + executionTimeMs +
            '}';
    }
}
