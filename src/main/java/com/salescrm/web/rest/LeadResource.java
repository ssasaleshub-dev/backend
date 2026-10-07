package com.salescrm.web.rest;

import com.salescrm.repository.LeadRepository;
import com.salescrm.service.LeadService;
import com.salescrm.service.dto.LeadDTO;
import com.salescrm.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ForwardedHeaderUtils;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.salescrm.domain.Lead}.
 */
@RestController
@RequestMapping("/api/leads")
public class LeadResource {

    private static final Logger LOG = LoggerFactory.getLogger(LeadResource.class);

    private static final String ENTITY_NAME = "lead";

    @Value("${jhipster.clientApp.name:salesCRM}")
    private String applicationName;

    private final LeadService leadService;

    private final LeadRepository leadRepository;

    public LeadResource(LeadService leadService, LeadRepository leadRepository) {
        this.leadService = leadService;
        this.leadRepository = leadRepository;
    }

    /**
     * {@code POST  /leads} : Create a new lead.
     *
     * @param leadDTO the leadDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new leadDTO, or with status {@code 400 (Bad Request)} if the lead has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<LeadDTO>> createLead(@Valid @RequestBody LeadDTO leadDTO) throws URISyntaxException {
        LOG.debug("REST request to save Lead : {}", leadDTO);
        if (leadDTO.getId() != null) {
            throw new BadRequestAlertException("A new lead cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return leadService
            .save(leadDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/leads/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /leads/:id} : Updates an existing lead.
     *
     * @param id the id of the leadDTO to save.
     * @param leadDTO the leadDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated leadDTO,
     * or with status {@code 400 (Bad Request)} if the leadDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the leadDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<LeadDTO>> updateLead(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody LeadDTO leadDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Lead : {}, {}", id, leadDTO);
        if (leadDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, leadDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return leadRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return leadService
                    .update(leadDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /leads/:id} : Partial updates given fields of an existing lead, field will ignore if it is null
     *
     * @param id the id of the leadDTO to save.
     * @param leadDTO the leadDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated leadDTO,
     * or with status {@code 400 (Bad Request)} if the leadDTO is not valid,
     * or with status {@code 404 (Not Found)} if the leadDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the leadDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<LeadDTO>> partialUpdateLead(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody LeadDTO leadDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Lead partially : {}, {}", id, leadDTO);
        if (leadDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, leadDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return leadRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<LeadDTO> result = leadService.partialUpdate(leadDTO);

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
     * {@code GET  /leads} : get all the Leads.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Leads in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<LeadDTO>>> getAllLeads(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request,
        @RequestParam(name = "filter", required = false) String filter,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        if ("leadpreference-is-null".equals(filter)) {
            LOG.debug("REST request to get all Leads where leadPreference is null");
            return leadService.findAllWhereLeadPreferenceIsNull().collectList().map(ResponseEntity::ok);
        }
        LOG.debug("REST request to get a page of Leads");
        return leadService
            .countAll()
            .zipWith(leadService.findAll(pageable).collectList())
            .map(countWithEntities ->
                ResponseEntity.ok()
                    .headers(
                        PaginationUtil.generatePaginationHttpHeaders(
                            ForwardedHeaderUtils.adaptFromForwardedHeaders(request.getURI(), request.getHeaders()),
                            new PageImpl<>(countWithEntities.getT2(), pageable, countWithEntities.getT1())
                        )
                    )
                    .body(countWithEntities.getT2())
            );
    }

    /**
     * {@code GET  /leads/:id} : get the "id" lead.
     *
     * @param id the id of the leadDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the leadDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<LeadDTO>> getLead(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Lead : {}", id);
        Mono<LeadDTO> leadDTO = leadService.findOne(id);
        return ResponseUtil.wrapOrNotFound(leadDTO);
    }

    /**
     * {@code DELETE  /leads/:id} : delete the "id" lead.
     *
     * @param id the id of the leadDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteLead(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Lead : {}", id);
        return leadService
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
