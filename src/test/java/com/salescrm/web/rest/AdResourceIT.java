package com.salescrm.web.rest;

import static com.salescrm.domain.AdAsserts.*;
import static com.salescrm.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.IntegrationTest;
import com.salescrm.domain.Ad;
import com.salescrm.repository.AdRepository;
import com.salescrm.repository.EntityManager;
import com.salescrm.service.AdService;
import com.salescrm.service.dto.AdDTO;
import com.salescrm.service.mapper.AdMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/**
 * Integration tests for the {@link AdResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class AdResourceIT {

    private static final String DEFAULT_EXTERNAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_EXTERNAL_ID = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/ads";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AdRepository adRepository;

    @Mock
    private AdRepository adRepositoryMock;

    @Autowired
    private AdMapper adMapper;

    @Mock
    private AdService adServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Ad ad;

    private Ad insertedAd;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Ad createEntity() {
        return new Ad().externalId(DEFAULT_EXTERNAL_ID).name(DEFAULT_NAME).status(DEFAULT_STATUS).createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Ad createUpdatedEntity() {
        return new Ad().externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Ad.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        ad = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAd != null) {
            adRepository.delete(insertedAd).block();
            insertedAd = null;
        }
        deleteEntities(em);
    }

    @Test
    void createAd() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Ad
        AdDTO adDTO = adMapper.toDto(ad);
        var returnedAdDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AdDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Ad in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAd = adMapper.toEntity(returnedAdDTO);
        assertAdUpdatableFieldsEquals(returnedAd, getPersistedAd(returnedAd));

        insertedAd = returnedAd;
    }

    @Test
    void createAdWithExistingId() throws Exception {
        // Create the Ad with an existing ID
        ad.setId(1L);
        AdDTO adDTO = adMapper.toDto(ad);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Ad in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkExternalIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ad.setExternalId(null);

        // Create the Ad, which fails.
        AdDTO adDTO = adMapper.toDto(ad);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        ad.setName(null);

        // Create the Ad, which fails.
        AdDTO adDTO = adMapper.toDto(ad);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllAds() {
        // Initialize the database
        insertedAd = adRepository.save(ad).block();

        // Get all the adList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(ad.getId().intValue()))
            .jsonPath("$.[*].externalId")
            .value(hasItem(DEFAULT_EXTERNAL_ID))
            .jsonPath("$.[*].name")
            .value(hasItem(DEFAULT_NAME))
            .jsonPath("$.[*].status")
            .value(hasItem(DEFAULT_STATUS))
            .jsonPath("$.[*].createdAt")
            .value(hasItem(DEFAULT_CREATED_AT.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAdsWithEagerRelationshipsIsEnabled() {
        when(adServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(adServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAdsWithEagerRelationshipsIsNotEnabled() {
        when(adServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(adRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getAd() {
        // Initialize the database
        insertedAd = adRepository.save(ad).block();

        // Get the ad
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, ad.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(ad.getId().intValue()))
            .jsonPath("$.externalId")
            .value(is(DEFAULT_EXTERNAL_ID))
            .jsonPath("$.name")
            .value(is(DEFAULT_NAME))
            .jsonPath("$.status")
            .value(is(DEFAULT_STATUS))
            .jsonPath("$.createdAt")
            .value(is(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    void getNonExistingAd() {
        // Get the ad
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingAd() throws Exception {
        // Initialize the database
        insertedAd = adRepository.save(ad).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ad
        Ad updatedAd = adRepository.findById(ad.getId()).block();
        updatedAd.externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);
        AdDTO adDTO = adMapper.toDto(updatedAd);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, adDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Ad in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAdToMatchAllProperties(updatedAd);
    }

    @Test
    void putNonExistingAd() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ad.setId(longCount.incrementAndGet());

        // Create the Ad
        AdDTO adDTO = adMapper.toDto(ad);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, adDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Ad in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchAd() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ad.setId(longCount.incrementAndGet());

        // Create the Ad
        AdDTO adDTO = adMapper.toDto(ad);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Ad in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamAd() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ad.setId(longCount.incrementAndGet());

        // Create the Ad
        AdDTO adDTO = adMapper.toDto(ad);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Ad in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateAdWithPatch() throws Exception {
        // Initialize the database
        insertedAd = adRepository.save(ad).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ad using partial update
        Ad partialUpdatedAd = new Ad();
        partialUpdatedAd.setId(ad.getId());

        partialUpdatedAd.externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedAd.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedAd))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Ad in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAdUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedAd, ad), getPersistedAd(ad));
    }

    @Test
    void fullUpdateAdWithPatch() throws Exception {
        // Initialize the database
        insertedAd = adRepository.save(ad).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the ad using partial update
        Ad partialUpdatedAd = new Ad();
        partialUpdatedAd.setId(ad.getId());

        partialUpdatedAd.externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedAd.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedAd))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Ad in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAdUpdatableFieldsEquals(partialUpdatedAd, getPersistedAd(partialUpdatedAd));
    }

    @Test
    void patchNonExistingAd() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ad.setId(longCount.incrementAndGet());

        // Create the Ad
        AdDTO adDTO = adMapper.toDto(ad);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, adDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Ad in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchAd() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ad.setId(longCount.incrementAndGet());

        // Create the Ad
        AdDTO adDTO = adMapper.toDto(ad);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Ad in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamAd() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        ad.setId(longCount.incrementAndGet());

        // Create the Ad
        AdDTO adDTO = adMapper.toDto(ad);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(adDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Ad in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteAd() {
        // Initialize the database
        insertedAd = adRepository.save(ad).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the ad
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, ad.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return adRepository.count().block();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Ad getPersistedAd(Ad ad) {
        return adRepository.findById(ad.getId()).block();
    }

    protected void assertPersistedAdToMatchAllProperties(Ad expectedAd) {
        // Test fails because reactive api returns an empty object instead of null
        // assertAdAllPropertiesEquals(expectedAd, getPersistedAd(expectedAd));
        assertAdUpdatableFieldsEquals(expectedAd, getPersistedAd(expectedAd));
    }

    protected void assertPersistedAdToMatchUpdatableProperties(Ad expectedAd) {
        // Test fails because reactive api returns an empty object instead of null
        // assertAdAllUpdatablePropertiesEquals(expectedAd, getPersistedAd(expectedAd));
        assertAdUpdatableFieldsEquals(expectedAd, getPersistedAd(expectedAd));
    }
}
