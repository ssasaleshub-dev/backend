package com.salescrm.service.impl;

import com.salescrm.domain.CrmEvent;
import com.salescrm.repository.CrmEventRepository;
import com.salescrm.service.CrmEventService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link CrmEvent}.
 */
@Service
@Transactional
public class CrmEventServiceImpl implements CrmEventService {

    private static final Logger LOG = LoggerFactory.getLogger(CrmEventServiceImpl.class);

    private final CrmEventRepository crmEventRepository;

    public CrmEventServiceImpl(CrmEventRepository crmEventRepository) {
        this.crmEventRepository = crmEventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<CrmEvent> getEvents(String userId, Long candidateId) {
        LOG.debug("Request to get events. userId: {}, candidateId: {}", userId, candidateId);
        if (candidateId != null) {
            return crmEventRepository.findAllByCandidateId(candidateId);
        }
        if (userId != null && !userId.isBlank()) {
            return crmEventRepository.findAllByUserIdOrderByEventDateAscEventTimeAsc(userId);
        }
        return crmEventRepository.findAllByOrderByEventDateAscEventTimeAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<CrmEvent> getEventById(UUID id) {
        LOG.debug("Request to get CrmEvent : {}", id);
        return crmEventRepository.findById(id).map(CrmEvent::setIsPersisted);
    }

    @Override
    public Mono<CrmEvent> createEvent(CrmEvent event) {
        LOG.debug("Request to create CrmEvent : {}", event);
        if (event.getId() == null) {
            event.setId(UUID.randomUUID());
        }
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(Instant.now());
        }
        if (event.getUpdatedAt() == null) {
            event.setUpdatedAt(Instant.now());
        }
        if (event.getIsCompleted() == null) {
            event.setIsCompleted(Boolean.FALSE);
        }
        if (event.getNotificationSent() == null) {
            event.setNotificationSent(Boolean.FALSE);
        }
        if (event.getRemindBeforeMinutes() == null) {
            event.setRemindBeforeMinutes(15);
        }
        return crmEventRepository.save(event).map(CrmEvent::setIsPersisted);
    }

    @Override
    public Mono<CrmEvent> updateEvent(UUID id, CrmEvent event) {
        LOG.debug("Request to update CrmEvent : {}, data: {}", id, event);
        return crmEventRepository
            .findById(id)
            .flatMap(existing -> {
                existing.setIsPersisted();
                if (event.getTitle() != null) {
                    existing.setTitle(event.getTitle());
                }
                if (event.getDescription() != null) {
                    existing.setDescription(event.getDescription());
                }
                if (event.getEventDate() != null) {
                    existing.setEventDate(event.getEventDate());
                }
                if (event.getEventTime() != null) {
                    existing.setEventTime(event.getEventTime());
                }
                if (event.getEventType() != null) {
                    existing.setEventType(event.getEventType());
                }
                if (event.getPriority() != null) {
                    existing.setPriority(event.getPriority());
                }
                if (event.getCandidateId() != null) {
                    existing.setCandidateId(event.getCandidateId());
                }
                if (event.getCandidateName() != null) {
                    existing.setCandidateName(event.getCandidateName());
                }
                if (event.getIsCompleted() != null) {
                    existing.setIsCompleted(event.getIsCompleted());
                }
                if (event.getRemindBeforeMinutes() != null) {
                    existing.setRemindBeforeMinutes(event.getRemindBeforeMinutes());
                }
                existing.setUpdatedAt(Instant.now());
                return crmEventRepository.save(existing);
            })
            .map(CrmEvent::setIsPersisted);
    }

    @Override
    public Mono<CrmEvent> toggleComplete(UUID id, Boolean isCompleted) {
        LOG.debug("Request to toggle complete on CrmEvent : {}, value: {}", id, isCompleted);
        return crmEventRepository
            .findById(id)
            .flatMap(existing -> {
                existing.setIsPersisted();
                if (isCompleted != null) {
                    existing.setIsCompleted(isCompleted);
                } else {
                    existing.setIsCompleted(!Boolean.TRUE.equals(existing.getIsCompleted()));
                }
                existing.setUpdatedAt(Instant.now());
                return crmEventRepository.save(existing);
            })
            .map(CrmEvent::setIsPersisted);
    }

    @Override
    public Mono<Void> deleteEvent(UUID id) {
        LOG.debug("Request to delete CrmEvent : {}", id);
        return crmEventRepository.deleteById(id);
    }
}
