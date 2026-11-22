package com.salescrm.security;

import java.util.Collections;
import java.util.Set;

/**
 * Which leads the current user may see or change.
 * <ul>
 *   <li>Admin: every lead (an optional visa-type filter narrows the list).</li>
 *   <li>Desk staff (work visa, study visa, domestic college admission): leads of their desk's
 *       visa type, plus any lead assigned to them.</li>
 *   <li>Anyone else (no desk role, or not logged in): only leads assigned to them.</li>
 * </ul>
 */
public final class LeadAccessScope {

    private final String login;
    private final boolean admin;
    private final boolean workVisa;
    private final boolean studyVisa;
    private final boolean domesticAdmission;

    public LeadAccessScope(String login, Set<String> authorities) {
        Set<String> auths = authorities != null ? authorities : Collections.emptySet();
        this.login = login != null ? login.trim() : "";
        this.admin = auths.contains(AuthoritiesConstants.ADMIN);
        this.workVisa = auths.contains(AuthoritiesConstants.WORK_VISA);
        this.studyVisa = auths.contains(AuthoritiesConstants.STUDY_VISA);
        this.domesticAdmission = auths.contains(AuthoritiesConstants.DOMESTIC_COLLEGE_ADMISSION);
    }

    public String getLogin() {
        return login;
    }

    public boolean isAdmin() {
        return admin;
    }

    /** True when this user may access a lead with the given visa type and assignee. */
    public boolean canAccess(String visaType, String assignedTo) {
        return isVisible(visaType, assignedTo, null);
    }

    /**
     * True when the lead is visible to this user. {@code requestedVisaType} is an optional list
     * filter; for non-admins it can only narrow results, never widen them.
     */
    public boolean isVisible(String visaType, String assignedTo, String requestedVisaType) {
        boolean filtered = requestedVisaType != null && !requestedVisaType.isBlank() && !"ALL".equalsIgnoreCase(requestedVisaType);

        if (admin) {
            return !filtered || matchesVisaType(visaType, requestedVisaType);
        }

        boolean assignedToMe = !login.isEmpty() && login.equalsIgnoreCase(assignedTo != null ? assignedTo.trim() : null);
        boolean inMyDesk =
            (workVisa && matchesVisaType(visaType, "WORK_VISA")) ||
            (studyVisa && matchesVisaType(visaType, "STUDY_VISA")) ||
            (domesticAdmission && matchesVisaType(visaType, "DOMESTIC_COLLEGE_ADMISSION"));

        if (!assignedToMe && !inMyDesk) {
            return false;
        }
        // A counselor's own assigned leads stay visible regardless of the desk filter
        return assignedToMe || !filtered || matchesVisaType(visaType, requestedVisaType);
    }

    public static boolean matchesVisaType(String actual, String target) {
        if (actual == null || target == null) return false;
        String a = actual.trim().toUpperCase();
        String t = target.trim().toUpperCase();
        if (a.equals(t)) return true;
        if (t.contains("WORK") && a.contains("WORK")) return true;
        if (t.contains("STUDY") && (a.contains("STUDY") || a.contains("STUDENT"))) return true;
        return (
            (t.contains("DOMESTIC") || t.contains("COLLEGE") || t.contains("ADMISSION")) &&
            (a.contains("DOMESTIC") || a.contains("COLLEGE") || a.contains("ADMISSION"))
        );
    }
}
