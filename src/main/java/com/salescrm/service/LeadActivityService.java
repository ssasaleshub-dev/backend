package com.salescrm.service;

import com.salescrm.domain.LeadActivity;
import com.salescrm.repository.LeadActivityRepository;
import com.salescrm.repository.UserRepository;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.dto.LeadActivityDTO;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for managing {@link LeadActivity} audit logging and candidate interaction history.
 */
@Service
@Transactional
public class LeadActivityService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadActivityService.class);

    private final LeadActivityRepository leadActivityRepository;
    private final UserRepository userRepository;

    public LeadActivityService(LeadActivityRepository leadActivityRepository, UserRepository userRepository) {
        this.leadActivityRepository = leadActivityRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all activities/audit logs for a specific lead ordered by creation date descending.
     *
     * @param leadId the id of the lead
     * @return the list of activities
     */
    @Transactional(readOnly = true)
    public Flux<LeadActivity> getActivitiesForLead(Long leadId) {
        LOG.debug("Request to get activities for Lead : {}", leadId);
        return leadActivityRepository.findAllByLeadIdOrderByCreatedAtDesc(leadId);
    }

    /**
     * Records a new activity log entry for a lead with authenticated counselor attribution.
     *
     * @param dto the activity log details
     * @return the saved LeadActivity
     */
    public Mono<LeadActivity> logActivity(LeadActivityDTO dto) {
        LOG.debug("Request to log LeadActivity : {}", dto);

        return SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login)
                .map(user -> {
                    String displayName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                        (user.getLastName() != null ? user.getLastName() : "")).trim();
                    if (displayName.isEmpty()) {
                        displayName = user.getLogin();
                    }
                    return new UserContext(String.valueOf(user.getId()), displayName);
                })
                .defaultIfEmpty(new UserContext(login, login))
            )
            .defaultIfEmpty(new UserContext(
                dto.getUserId() != null && !dto.getUserId().isBlank() ? dto.getUserId() : "system",
                dto.getUserName() != null && !dto.getUserName().isBlank() ? dto.getUserName() : "Counselor"
            ))
            .flatMap(userContext -> {
                LeadActivity activity = new LeadActivity();
                activity.setLeadId(dto.getLeadId());
                activity.setUserId(dto.getUserId() != null && !dto.getUserId().isBlank() ? dto.getUserId() : userContext.userId);
                activity.setUserName(dto.getUserName() != null && !dto.getUserName().isBlank() ? dto.getUserName() : userContext.userName);
                activity.setActionType(dto.getActionType() != null && !dto.getActionType().isBlank() ? dto.getActionType() : "NOTE_ADDED");
                activity.setSummary(dto.getSummary());
                activity.setDetails(dto.getDetails());
                activity.setFieldName(dto.getFieldName());
                activity.setOldValue(dto.getOldValue());
                activity.setNewValue(dto.getNewValue());
                activity.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : Instant.now());

                return leadActivityRepository.save(activity);
            });
    }

    private record UserContext(String userId, String userName) {}
}
