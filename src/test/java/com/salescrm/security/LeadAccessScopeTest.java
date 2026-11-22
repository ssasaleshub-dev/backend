package com.salescrm.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class LeadAccessScopeTest {

    private static LeadAccessScope scope(String login, String... authorities) {
        return new LeadAccessScope(login, Set.of(authorities));
    }

    @Test
    void adminSeesEverythingAndFilterNarrows() {
        LeadAccessScope admin = scope("admin", AuthoritiesConstants.ADMIN);
        assertThat(admin.canAccess("STUDY_VISA", null)).isTrue();
        assertThat(admin.canAccess(null, null)).isTrue();
        assertThat(admin.isVisible("STUDY_VISA", null, "WORK_VISA")).isFalse();
        assertThat(admin.isVisible("WORK_VISA", null, "WORK_VISA")).isTrue();
    }

    @Test
    void deskStaffSeeTheirDeskAndTheirAssignedLeads() {
        LeadAccessScope work = scope("ravi", AuthoritiesConstants.WORK_VISA);
        assertThat(work.canAccess("WORK_VISA", null)).isTrue();
        assertThat(work.canAccess("STUDY_VISA", null)).isFalse();
        assertThat(work.canAccess("STUDY_VISA", "Ravi")).isTrue();

        LeadAccessScope domestic = scope("meena", AuthoritiesConstants.DOMESTIC_COLLEGE_ADMISSION);
        assertThat(domestic.canAccess("DOMESTIC_COLLEGE_ADMISSION", null)).isTrue();
        assertThat(domestic.canAccess("WORK_VISA", null)).isFalse();
    }

    @Test
    void filterCannotWidenAccessButAssignedLeadsStayVisible() {
        LeadAccessScope work = scope("ravi", AuthoritiesConstants.WORK_VISA);
        assertThat(work.isVisible("STUDY_VISA", null, "STUDY_VISA")).isFalse();
        assertThat(work.isVisible("STUDY_VISA", "ravi", "WORK_VISA")).isTrue();
    }

    @Test
    void usersWithoutDeskRoleOnlySeeAssignedLeads() {
        LeadAccessScope plain = scope("guest", AuthoritiesConstants.USER);
        assertThat(plain.canAccess("WORK_VISA", null)).isFalse();
        assertThat(plain.canAccess("WORK_VISA", "guest")).isTrue();

        LeadAccessScope anonymous = scope("", AuthoritiesConstants.ANONYMOUS);
        assertThat(anonymous.canAccess("WORK_VISA", null)).isFalse();
        assertThat(anonymous.canAccess("WORK_VISA", "")).isFalse();
    }
}
