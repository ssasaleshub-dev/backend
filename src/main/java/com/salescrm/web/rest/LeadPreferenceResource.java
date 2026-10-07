package com.salescrm.web.rest;

import com.salescrm.repository.LeadPreferenceRepository;
import com.salescrm.service.LeadPreferenceService;
import com.salescrm.service.dto.LeadPreferenceDTO;
import com.salescrm.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.salescrm.domain.LeadPreference}.
 */
@RestController
@RequestMapping("/api/lead-preferences")
public class LeadPreferenceResource {

    private static final Logger LOG = LoggerFactory.getLogger(LeadPreferenceResource.class);

    private static final String ENTITY_NAME = "leadPreference";

    @Value("${jhipster.clientApp.name:salesCRM}")
    private String applicationName;

    private final LeadPreferenceService leadPreferenceService;

    private final LeadPreferenceRepository leadPreferenceRepository;

    public LeadPreferenceResource(LeadPreferenceService leadPreferenceService, LeadPreferenceRepository leadPreferenceRepository) {
        this.leadPreferenceService = leadPreferenceService;
        this.leadPreferenceRepository = leadPreferenceRepository;
    }

    /**
     * {@code POST  /lead-preferences} : Create a new leadPreference.
     *
     * @param leadPreferenceDTO the leadPreferenceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new leadPreferenceDTO, or with status {@code 400 (Bad Request)} if the leadPreference has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<LeadPreferenceDTO>> createLeadPreference(@RequestBody LeadPreferenceDTO leadPreferenceDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save LeadPreference : {}", leadPreferenceDTO);
        if (leadPreferenceDTO.getId() != null) {
            throw new BadRequestAlertException("A new leadPreference cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return leadPreferenceService
            .save(leadPreferenceDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/lead-preferences/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /lead-preferences/:id} : Updates an existing leadPreference.
     *
     * @param id the id of the leadPreferenceDTO to save.
     * @param leadPreferenceDTO the leadPreferenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated leadPreferenceDTO,
     * or with status {@code 400 (Bad Request)} if the leadPreferenceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the leadPreferenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<LeadPreferenceDTO>> updateLeadPreference(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody LeadPreferenceDTO leadPreferenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update LeadPreference : {}, {}", id, leadPreferenceDTO);
        if (leadPreferenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, leadPreferenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return leadPreferenceRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return leadPreferenceService
                    .update(leadPreferenceDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /lead-preferences/:id} : Partial updates given fields of an existing leadPreference, field will ignore if it is null
     *
     * @param id the id of the leadPreferenceDTO to save.
     * @param leadPreferenceDTO the leadPreferenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated leadPreferenceDTO,
     * or with status {@code 400 (Bad Request)} if the leadPreferenceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the leadPreferenceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the leadPreferenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<LeadPreferenceDTO>> partialUpdateLeadPreference(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody LeadPreferenceDTO leadPreferenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update LeadPreference partially : {}, {}", id, leadPreferenceDTO);
        if (leadPreferenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, leadPreferenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return leadPreferenceRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<LeadPreferenceDTO> result = leadPreferenceService.partialUpdate(leadPreferenceDTO);

                return result
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(res ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, res.getId().toString()))
                            .body(res)
                    );
            });
    }

    /**
     * {@code GET  /lead-preferences} : get all the Lead Preferences.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Lead Preferences in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<LeadPreferenceDTO>> getAllLeadPreferences(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all LeadPreferences");
        return leadPreferenceService.findAll().collectList();
    }

    /**
     * {@code GET  /lead-preferences} : get all the Lead Preferences as a stream.
     * @return the {@link Flux} of Lead Preferences.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<LeadPreferenceDTO> getAllLeadPreferencesAsStream() {
        LOG.debug("REST request to get all LeadPreferences as a stream");
        return leadPreferenceService.findAll();
    }

    /**
     * {@code GET  /lead-preferences/:id} : get the "id" leadPreference.
     *
     * @param id the id of the leadPreferenceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the leadPreferenceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<LeadPreferenceDTO>> getLeadPreference(@PathVariable("id") Long id) {
        LOG.debug("REST request to get LeadPreference : {}", id);
        Mono<LeadPreferenceDTO> leadPreferenceDTO = leadPreferenceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(leadPreferenceDTO);
    }

    /**
     * {@code DELETE  /lead-preferences/:id} : delete the "id" leadPreference.
     *
     * @param id the id of the leadPreferenceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteLeadPreference(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete LeadPreference : {}", id);
        return leadPreferenceService
            .delete(id)
            .then(
                Mono.just(
                    ResponseEntity.noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                        .build()
                )
            );
    }
}
