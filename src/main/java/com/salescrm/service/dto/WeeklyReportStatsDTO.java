package com.salescrm.service.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executive weekly performance metrics for admin digest emails.
 * STRICT DATA PRIVACY: Contains ONLY counts, rates, percentages, and aggregate trend arrays.
 * NEVER contains candidate or customer personal details.
 */
public class WeeklyReportStatsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dateRange;
    private long totalLeads;
    private long prevWeekLeads;
    private double growthRate;

    private long contactedCount;
    private double contactRate;

    private long convertedCount;
    private double conversionRate;

    private long qualifiedCount;
    private long newCount;
    private long lostCount;

    private Map<String, Long> statusBreakdown = new LinkedHashMap<>();
    private Map<String, Long> platformBreakdown = new LinkedHashMap<>();

    private int[] dailyCounts = new int[7];
    private String[] dailyLabels = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    public WeeklyReportStatsDTO() {}

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public long getTotalLeads() {
        return totalLeads;
    }

    public void setTotalLeads(long totalLeads) {
        this.totalLeads = totalLeads;
    }

    public long getPrevWeekLeads() {
        return prevWeekLeads;
    }

    public void setPrevWeekLeads(long prevWeekLeads) {
        this.prevWeekLeads = prevWeekLeads;
    }

    public double getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(double growthRate) {
        this.growthRate = growthRate;
    }

    public long getContactedCount() {
        return contactedCount;
    }

    public void setContactedCount(long contactedCount) {
        this.contactedCount = contactedCount;
    }

    public double getContactRate() {
        return contactRate;
    }

    public void setContactRate(double contactRate) {
        this.contactRate = contactRate;
    }

    public long getConvertedCount() {
        return convertedCount;
    }

    public void setConvertedCount(long convertedCount) {
        this.convertedCount = convertedCount;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public long getQualifiedCount() {
        return qualifiedCount;
    }

    public void setQualifiedCount(long qualifiedCount) {
        this.qualifiedCount = qualifiedCount;
    }

    public long getNewCount() {
        return newCount;
    }

    public void setNewCount(long newCount) {
        this.newCount = newCount;
    }

    public long getLostCount() {
        return lostCount;
    }

    public void setLostCount(long lostCount) {
        this.lostCount = lostCount;
    }

    public Map<String, Long> getStatusBreakdown() {
        return statusBreakdown;
    }

    public void setStatusBreakdown(Map<String, Long> statusBreakdown) {
        this.statusBreakdown = statusBreakdown;
    }

    public Map<String, Long> getPlatformBreakdown() {
        return platformBreakdown;
    }

    public void setPlatformBreakdown(Map<String, Long> platformBreakdown) {
        this.platformBreakdown = platformBreakdown;
    }

    public int[] getDailyCounts() {
        return dailyCounts;
    }

    public void setDailyCounts(int[] dailyCounts) {
        this.dailyCounts = dailyCounts;
    }

    public String[] getDailyLabels() {
        return dailyLabels;
    }

    public void setDailyLabels(String[] dailyLabels) {
        this.dailyLabels = dailyLabels;
    }
}
