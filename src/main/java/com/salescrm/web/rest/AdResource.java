package com.salescrm.web.rest;

import com.salescrm.repository.AdRepository;
import com.salescrm.service.AdService;
import com.salescrm.service.dto.AdDTO;
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
 * REST controller for managing {@link com.salescrm.domain.Ad}.
 */
@RestController
@RequestMapping("/api/ads")
public class AdResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdResource.class);

    private static final String ENTITY_NAME = "ad";

    @Value("${jhipster.clientApp.name:salesCRM}")
    private String applicationName;

    private final AdService adService;

    private final AdRepository adRepository;

    public AdResource(AdService adService, AdRepository adRepository) {
        this.adService = adService;
        this.adRepository = adRepository;
    }

    /**
     * {@code POST  /ads} : Create a new ad.
     *
     * @param adDTO the adDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new adDTO, or with status {@code 400 (Bad Request)} if the ad has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<AdDTO>> createAd(@Valid @RequestBody AdDTO adDTO) throws URISyntaxException {
        LOG.debug("REST request to save Ad : {}", adDTO);
        if (adDTO.getId() != null) {
            throw new BadRequestAlertException("A new ad cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return adService
            .save(adDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/ads/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /ads/:id} : Updates an existing ad.
     *
     * @param id the id of the adDTO to save.
     * @param adDTO the adDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated adDTO,
     * or with status {@code 400 (Bad Request)} if the adDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the adDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<AdDTO>> updateAd(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody AdDTO adDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Ad : {}, {}", id, adDTO);
        if (adDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, adDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return adRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return adService
                    .update(adDTO)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /ads/:id} : Partial updates given fields of an existing ad, field will ignore if it is null
     *
     * @param id the id of the adDTO to save.
     * @param adDTO the adDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated adDTO,
     * or with status {@code 400 (Bad Request)} if the adDTO is not valid,
     * or with status {@code 404 (Not Found)} if the adDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the adDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<AdDTO>> partialUpdateAd(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody AdDTO adDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Ad partially : {}, {}", id, adDTO);
        if (adDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, adDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return adRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<AdDTO> result = adService.partialUpdate(adDTO);

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
     * {@code GET  /ads} : get all the Ads.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Ads in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<AdDTO>>> getAllAds(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of Ads");
        return adService
            .countAll()
            .zipWith(adService.findAll(pageable).collectList())
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
     * {@code GET  /ads/:id} : get the "id" ad.
     *
     * @param id the id of the adDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the adDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AdDTO>> getAd(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Ad : {}", id);
        Mono<AdDTO> adDTO = adService.findOne(id);
        return ResponseUtil.wrapOrNotFound(adDTO);
    }

    /**
     * {@code DELETE  /ads/:id} : delete the "id" ad.
     *
     * @param id the id of the adDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteAd(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Ad : {}", id);
        return adService
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
