package com.salescrm.web.rest;

import com.salescrm.repository.LeadFormRepository;
import com.salescrm.service.LeadFormService;
import com.salescrm.service.dto.LeadFormDTO;
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
 * REST controller for managing {@link com.salescrm.domain.LeadForm}.
 */
@RestController
@RequestMapping("/api/lead-forms")
public class LeadFormResource {

    private static final Logger LOG = LoggerFactory.getLogger(LeadFormResource.class);

    private static final String ENTITY_NAME = "leadForm";

    @Value("${jhipster.clientApp.name:salesCRM}")
    private String applicationName;

    private final LeadFormService leadFormService;

    private final LeadFormRepository leadFormRepository;

    public LeadFormResource(LeadFormService leadFormService, LeadFormRepository leadFormRepository) {
        this.leadFormService = leadFormService;
        this.leadFormRepository = leadFormRepository;
    }

    /**
     * {@code POST  /lead-forms} : Create a new leadForm.
     *
     * @param leadFormDTO the leadFormDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new leadFormDTO, or with status {@code 400 (Bad Request)} if the leadForm has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<LeadFormDTO>> createLeadForm(@Valid @RequestBody LeadFormDTO leadFormDTO) throws URISyntaxException {
        LOG.debug("REST request to save LeadForm : {}", leadFormDTO);
        if (leadFormDTO.getId() != null) {
            throw new BadRequestAlertException("A new leadForm cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return leadFormService
            .save(leadFormDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/lead-forms/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /lead-forms/:id} : Updates an existing leadForm.
     *
     * @param id the id of the leadFormDTO to save.
     * @param leadFormDTO the leadFormDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated leadFormDTO,
     * or with status {@code 400 (Bad Request)} if the leadFormDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the leadFormDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<LeadFormDTO>> updateLeadForm(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody LeadFormDTO leadFormDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update LeadForm : {}, {}", id, leadFormDTO);
        if (leadFormDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, leadFormDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return leadFormRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return leadFormService
                    .update(leadFormDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /lead-forms/:id} : Partial updates given fields of an existing leadForm, field will ignore if it is null
     *
     * @param id the id of the leadFormDTO to save.
     * @param leadFormDTO the leadFormDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated leadFormDTO,
     * or with status {@code 400 (Bad Request)} if the leadFormDTO is not valid,
     * or with status {@code 404 (Not Found)} if the leadFormDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the leadFormDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<LeadFormDTO>> partialUpdateLeadForm(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody LeadFormDTO leadFormDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update LeadForm partially : {}, {}", id, leadFormDTO);
        if (leadFormDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, leadFormDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return leadFormRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<LeadFormDTO> result = leadFormService.partialUpdate(leadFormDTO);

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
     * {@code GET  /lead-forms} : get all the Lead Forms.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Lead Forms in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<LeadFormDTO>>> getAllLeadForms(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request
    ) {
        LOG.debug("REST request to get a page of LeadForms");
        return leadFormService
            .countAll()
            .zipWith(leadFormService.findAll(pageable).collectList())
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
     * {@code GET  /lead-forms/:id} : get the "id" leadForm.
     *
     * @param id the id of the leadFormDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the leadFormDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<LeadFormDTO>> getLeadForm(@PathVariable("id") Long id) {
        LOG.debug("REST request to get LeadForm : {}", id);
        Mono<LeadFormDTO> leadFormDTO = leadFormService.findOne(id);
        return ResponseUtil.wrapOrNotFound(leadFormDTO);
    }

    /**
     * {@code DELETE  /lead-forms/:id} : delete the "id" leadForm.
     *
     * @param id the id of the leadFormDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteLeadForm(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete LeadForm : {}", id);
        return leadFormService
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
