package com.salescrm.web.rest;

import com.salescrm.repository.AdSetRepository;
import com.salescrm.service.AdSetService;
import com.salescrm.service.dto.AdSetDTO;
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
 * REST controller for managing {@link com.salescrm.domain.AdSet}.
 */
@RestController
@RequestMapping("/api/ad-sets")
public class AdSetResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdSetResource.class);

    private static final String ENTITY_NAME = "adSet";

    @Value("${jhipster.clientApp.name:salesCRM}")
    private String applicationName;

    private final AdSetService adSetService;

    private final AdSetRepository adSetRepository;

    public AdSetResource(AdSetService adSetService, AdSetRepository adSetRepository) {
        this.adSetService = adSetService;
        this.adSetRepository = adSetRepository;
    }

    /**
     * {@code POST  /ad-sets} : Create a new adSet.
     *
     * @param adSetDTO the adSetDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new adSetDTO, or with status {@code 400 (Bad Request)} if the adSet has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<AdSetDTO>> createAdSet(@Valid @RequestBody AdSetDTO adSetDTO) throws URISyntaxException {
        LOG.debug("REST request to save AdSet : {}", adSetDTO);
        if (adSetDTO.getId() != null) {
            throw new BadRequestAlertException("A new adSet cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return adSetService
            .save(adSetDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/ad-sets/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /ad-sets/:id} : Updates an existing adSet.
     *
     * @param id the id of the adSetDTO to save.
     * @param adSetDTO the adSetDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated adSetDTO,
     * or with status {@code 400 (Bad Request)} if the adSetDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the adSetDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<AdSetDTO>> updateAdSet(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody AdSetDTO adSetDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AdSet : {}, {}", id, adSetDTO);
        if (adSetDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, adSetDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return adSetRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return adSetService
                    .update(adSetDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /ad-sets/:id} : Partial updates given fields of an existing adSet, field will ignore if it is null
     *
     * @param id the id of the adSetDTO to save.
     * @param adSetDTO the adSetDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated adSetDTO,
     * or with status {@code 400 (Bad Request)} if the adSetDTO is not valid,
     * or with status {@code 404 (Not Found)} if the adSetDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the adSetDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<AdSetDTO>> partialUpdateAdSet(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody AdSetDTO adSetDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AdSet partially : {}, {}", id, adSetDTO);
        if (adSetDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, adSetDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return adSetRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<AdSetDTO> result = adSetService.partialUpdate(adSetDTO);

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
     * {@code GET  /ad-sets} : get all the Ad Sets.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Ad Sets in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<AdSetDTO>>> getAllAdSets(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of AdSets");
        return adSetService
            .countAll()
            .zipWith(adSetService.findAll(pageable).collectList())
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
     * {@code GET  /ad-sets/:id} : get the "id" adSet.
     *
     * @param id the id of the adSetDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the adSetDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AdSetDTO>> getAdSet(@PathVariable("id") Long id) {
        LOG.debug("REST request to get AdSet : {}", id);
        Mono<AdSetDTO> adSetDTO = adSetService.findOne(id);
        return ResponseUtil.wrapOrNotFound(adSetDTO);
    }

    /**
     * {@code DELETE  /ad-sets/:id} : delete the "id" adSet.
     *
     * @param id the id of the adSetDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteAdSet(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete AdSet : {}", id);
        return adSetService
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
