package com.salescrm.web.rest;

import static com.salescrm.domain.AdSetAsserts.*;
import static com.salescrm.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.IntegrationTest;
import com.salescrm.domain.AdSet;
import com.salescrm.repository.AdSetRepository;
import com.salescrm.repository.EntityManager;
import com.salescrm.service.AdSetService;
import com.salescrm.service.dto.AdSetDTO;
import com.salescrm.service.mapper.AdSetMapper;
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
 * Integration tests for the {@link AdSetResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class AdSetResourceIT {

    private static final String DEFAULT_EXTERNAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_EXTERNAL_ID = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/ad-sets";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AdSetRepository adSetRepository;

    @Mock
    private AdSetRepository adSetRepositoryMock;

    @Autowired
    private AdSetMapper adSetMapper;

    @Mock
    private AdSetService adSetServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private AdSet adSet;

    private AdSet insertedAdSet;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AdSet createEntity() {
        return new AdSet().externalId(DEFAULT_EXTERNAL_ID).name(DEFAULT_NAME).status(DEFAULT_STATUS).createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AdSet createUpdatedEntity() {
        return new AdSet().externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(AdSet.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        adSet = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAdSet != null) {
            adSetRepository.delete(insertedAdSet).block();
            insertedAdSet = null;
        }
        deleteEntities(em);
    }

    @Test
    void createAdSet() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AdSet
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);
        var returnedAdSetDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AdSetDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the AdSet in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAdSet = adSetMapper.toEntity(returnedAdSetDTO);
        assertAdSetUpdatableFieldsEquals(returnedAdSet, getPersistedAdSet(returnedAdSet));

        insertedAdSet = returnedAdSet;
    }

    @Test
    void createAdSetWithExistingId() throws Exception {
        // Create the AdSet with an existing ID
        adSet.setId(1L);
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the AdSet in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkExternalIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        adSet.setExternalId(null);

        // Create the AdSet, which fails.
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        adSet.setName(null);

        // Create the AdSet, which fails.
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllAdSets() {
        // Initialize the database
        insertedAdSet = adSetRepository.save(adSet).block();

        // Get all the adSetList
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
            .value(hasItem(adSet.getId().intValue()))
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
    void getAllAdSetsWithEagerRelationshipsIsEnabled() {
        when(adSetServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(adSetServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAdSetsWithEagerRelationshipsIsNotEnabled() {
        when(adSetServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(adSetRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getAdSet() {
        // Initialize the database
        insertedAdSet = adSetRepository.save(adSet).block();

        // Get the adSet
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, adSet.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(adSet.getId().intValue()))
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
    void getNonExistingAdSet() {
        // Get the adSet
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingAdSet() throws Exception {
        // Initialize the database
        insertedAdSet = adSetRepository.save(adSet).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the adSet
        AdSet updatedAdSet = adSetRepository.findById(adSet.getId()).block();
        updatedAdSet.externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);
        AdSetDTO adSetDTO = adSetMapper.toDto(updatedAdSet);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, adSetDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the AdSet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAdSetToMatchAllProperties(updatedAdSet);
    }

    @Test
    void putNonExistingAdSet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adSet.setId(longCount.incrementAndGet());

        // Create the AdSet
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, adSetDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the AdSet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchAdSet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adSet.setId(longCount.incrementAndGet());

        // Create the AdSet
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the AdSet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamAdSet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adSet.setId(longCount.incrementAndGet());

        // Create the AdSet
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the AdSet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateAdSetWithPatch() throws Exception {
        // Initialize the database
        insertedAdSet = adSetRepository.save(adSet).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the adSet using partial update
        AdSet partialUpdatedAdSet = new AdSet();
        partialUpdatedAdSet.setId(adSet.getId());

        partialUpdatedAdSet.name(UPDATED_NAME);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedAdSet.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedAdSet))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the AdSet in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAdSetUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedAdSet, adSet), getPersistedAdSet(adSet));
    }

    @Test
    void fullUpdateAdSetWithPatch() throws Exception {
        // Initialize the database
        insertedAdSet = adSetRepository.save(adSet).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the adSet using partial update
        AdSet partialUpdatedAdSet = new AdSet();
        partialUpdatedAdSet.setId(adSet.getId());

        partialUpdatedAdSet.externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedAdSet.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedAdSet))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the AdSet in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAdSetUpdatableFieldsEquals(partialUpdatedAdSet, getPersistedAdSet(partialUpdatedAdSet));
    }

    @Test
    void patchNonExistingAdSet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adSet.setId(longCount.incrementAndGet());

        // Create the AdSet
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, adSetDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the AdSet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchAdSet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adSet.setId(longCount.incrementAndGet());

        // Create the AdSet
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the AdSet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamAdSet() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        adSet.setId(longCount.incrementAndGet());

        // Create the AdSet
        AdSetDTO adSetDTO = adSetMapper.toDto(adSet);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(adSetDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the AdSet in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteAdSet() {
        // Initialize the database
        insertedAdSet = adSetRepository.save(adSet).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the adSet
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, adSet.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return adSetRepository.count().block();
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

    protected AdSet getPersistedAdSet(AdSet adSet) {
        return adSetRepository.findById(adSet.getId()).block();
    }

    protected void assertPersistedAdSetToMatchAllProperties(AdSet expectedAdSet) {
        // Test fails because reactive api returns an empty object instead of null
        // assertAdSetAllPropertiesEquals(expectedAdSet, getPersistedAdSet(expectedAdSet));
        assertAdSetUpdatableFieldsEquals(expectedAdSet, getPersistedAdSet(expectedAdSet));
    }

    protected void assertPersistedAdSetToMatchUpdatableProperties(AdSet expectedAdSet) {
        // Test fails because reactive api returns an empty object instead of null
        // assertAdSetAllUpdatablePropertiesEquals(expectedAdSet, getPersistedAdSet(expectedAdSet));
        assertAdSetUpdatableFieldsEquals(expectedAdSet, getPersistedAdSet(expectedAdSet));
    }
}
