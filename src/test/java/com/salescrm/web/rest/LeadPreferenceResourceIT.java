package com.salescrm.web.rest;

import static com.salescrm.domain.LeadPreferenceAsserts.*;
import static com.salescrm.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.IntegrationTest;
import com.salescrm.domain.LeadPreference;
import com.salescrm.repository.EntityManager;
import com.salescrm.repository.LeadPreferenceRepository;
import com.salescrm.service.LeadPreferenceService;
import com.salescrm.service.dto.LeadPreferenceDTO;
import com.salescrm.service.mapper.LeadPreferenceMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
 * Integration tests for the {@link LeadPreferenceResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class LeadPreferenceResourceIT {

    private static final String DEFAULT_MATTRESS_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_MATTRESS_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_MATTRESS_SIZE = "AAAAAAAAAA";
    private static final String UPDATED_MATTRESS_SIZE = "BBBBBBBBBB";

    private static final String DEFAULT_BUDGET_RANGE = "AAAAAAAAAA";
    private static final String UPDATED_BUDGET_RANGE = "BBBBBBBBBB";

    private static final String DEFAULT_PURCHASE_TIMELINE = "AAAAAAAAAA";
    private static final String UPDATED_PURCHASE_TIMELINE = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/lead-preferences";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LeadPreferenceRepository leadPreferenceRepository;

    @Mock
    private LeadPreferenceRepository leadPreferenceRepositoryMock;

    @Autowired
    private LeadPreferenceMapper leadPreferenceMapper;

    @Mock
    private LeadPreferenceService leadPreferenceServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private LeadPreference leadPreference;

    private LeadPreference insertedLeadPreference;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static LeadPreference createEntity() {
        return new LeadPreference()
            .mattressType(DEFAULT_MATTRESS_TYPE)
            .mattressSize(DEFAULT_MATTRESS_SIZE)
            .budgetRange(DEFAULT_BUDGET_RANGE)
            .purchaseTimeline(DEFAULT_PURCHASE_TIMELINE)
            .createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static LeadPreference createUpdatedEntity() {
        return new LeadPreference()
            .mattressType(UPDATED_MATTRESS_TYPE)
            .mattressSize(UPDATED_MATTRESS_SIZE)
            .budgetRange(UPDATED_BUDGET_RANGE)
            .purchaseTimeline(UPDATED_PURCHASE_TIMELINE)
            .createdAt(UPDATED_CREATED_AT);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(LeadPreference.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        leadPreference = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedLeadPreference != null) {
            leadPreferenceRepository.delete(insertedLeadPreference).block();
            insertedLeadPreference = null;
        }
        deleteEntities(em);
    }

    @Test
    void createLeadPreference() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the LeadPreference
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(leadPreference);
        var returnedLeadPreferenceDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(LeadPreferenceDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the LeadPreference in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLeadPreference = leadPreferenceMapper.toEntity(returnedLeadPreferenceDTO);
        assertLeadPreferenceUpdatableFieldsEquals(returnedLeadPreference, getPersistedLeadPreference(returnedLeadPreference));

        insertedLeadPreference = returnedLeadPreference;
    }

    @Test
    void createLeadPreferenceWithExistingId() throws Exception {
        // Create the LeadPreference with an existing ID
        leadPreference.setId(1L);
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(leadPreference);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllLeadPreferencesAsStream() {
        // Initialize the database
        leadPreferenceRepository.save(leadPreference).block();

        List<LeadPreference> leadPreferenceList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(LeadPreferenceDTO.class)
            .getResponseBody()
            .map(leadPreferenceMapper::toEntity)
            .filter(leadPreference::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(leadPreferenceList).isNotNull();
        assertThat(leadPreferenceList).hasSize(1);
        LeadPreference testLeadPreference = leadPreferenceList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertLeadPreferenceAllPropertiesEquals(leadPreference, testLeadPreference);
        assertLeadPreferenceUpdatableFieldsEquals(leadPreference, testLeadPreference);
    }

    @Test
    void getAllLeadPreferences() {
        // Initialize the database
        insertedLeadPreference = leadPreferenceRepository.save(leadPreference).block();

        // Get all the leadPreferenceList
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
            .value(hasItem(leadPreference.getId().intValue()))
            .jsonPath("$.[*].mattressType")
            .value(hasItem(DEFAULT_MATTRESS_TYPE))
            .jsonPath("$.[*].mattressSize")
            .value(hasItem(DEFAULT_MATTRESS_SIZE))
            .jsonPath("$.[*].budgetRange")
            .value(hasItem(DEFAULT_BUDGET_RANGE))
            .jsonPath("$.[*].purchaseTimeline")
            .value(hasItem(DEFAULT_PURCHASE_TIMELINE))
            .jsonPath("$.[*].createdAt")
            .value(hasItem(DEFAULT_CREATED_AT.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLeadPreferencesWithEagerRelationshipsIsEnabled() {
        when(leadPreferenceServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(leadPreferenceServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLeadPreferencesWithEagerRelationshipsIsNotEnabled() {
        when(leadPreferenceServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(leadPreferenceRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getLeadPreference() {
        // Initialize the database
        insertedLeadPreference = leadPreferenceRepository.save(leadPreference).block();

        // Get the leadPreference
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, leadPreference.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(leadPreference.getId().intValue()))
            .jsonPath("$.mattressType")
            .value(is(DEFAULT_MATTRESS_TYPE))
            .jsonPath("$.mattressSize")
            .value(is(DEFAULT_MATTRESS_SIZE))
            .jsonPath("$.budgetRange")
            .value(is(DEFAULT_BUDGET_RANGE))
            .jsonPath("$.purchaseTimeline")
            .value(is(DEFAULT_PURCHASE_TIMELINE))
            .jsonPath("$.createdAt")
            .value(is(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    void getNonExistingLeadPreference() {
        // Get the leadPreference
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingLeadPreference() throws Exception {
        // Initialize the database
        insertedLeadPreference = leadPreferenceRepository.save(leadPreference).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the leadPreference
        LeadPreference updatedLeadPreference = leadPreferenceRepository.findById(leadPreference.getId()).block();
        updatedLeadPreference
            .mattressType(UPDATED_MATTRESS_TYPE)
            .mattressSize(UPDATED_MATTRESS_SIZE)
            .budgetRange(UPDATED_BUDGET_RANGE)
            .purchaseTimeline(UPDATED_PURCHASE_TIMELINE)
            .createdAt(UPDATED_CREATED_AT);
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(updatedLeadPreference);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, leadPreferenceDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the LeadPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLeadPreferenceToMatchAllProperties(updatedLeadPreference);
    }

    @Test
    void putNonExistingLeadPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadPreference.setId(longCount.incrementAndGet());

        // Create the LeadPreference
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(leadPreference);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, leadPreferenceDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchLeadPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadPreference.setId(longCount.incrementAndGet());

        // Create the LeadPreference
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(leadPreference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamLeadPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadPreference.setId(longCount.incrementAndGet());

        // Create the LeadPreference
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(leadPreference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the LeadPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateLeadPreferenceWithPatch() throws Exception {
        // Initialize the database
        insertedLeadPreference = leadPreferenceRepository.save(leadPreference).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the leadPreference using partial update
        LeadPreference partialUpdatedLeadPreference = new LeadPreference();
        partialUpdatedLeadPreference.setId(leadPreference.getId());

        partialUpdatedLeadPreference
            .mattressType(UPDATED_MATTRESS_TYPE)
            .mattressSize(UPDATED_MATTRESS_SIZE)
            .budgetRange(UPDATED_BUDGET_RANGE)
            .purchaseTimeline(UPDATED_PURCHASE_TIMELINE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedLeadPreference.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedLeadPreference))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the LeadPreference in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLeadPreferenceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedLeadPreference, leadPreference),
            getPersistedLeadPreference(leadPreference)
        );
    }

    @Test
    void fullUpdateLeadPreferenceWithPatch() throws Exception {
        // Initialize the database
        insertedLeadPreference = leadPreferenceRepository.save(leadPreference).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the leadPreference using partial update
        LeadPreference partialUpdatedLeadPreference = new LeadPreference();
        partialUpdatedLeadPreference.setId(leadPreference.getId());

        partialUpdatedLeadPreference
            .mattressType(UPDATED_MATTRESS_TYPE)
            .mattressSize(UPDATED_MATTRESS_SIZE)
            .budgetRange(UPDATED_BUDGET_RANGE)
            .purchaseTimeline(UPDATED_PURCHASE_TIMELINE)
            .createdAt(UPDATED_CREATED_AT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedLeadPreference.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedLeadPreference))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the LeadPreference in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLeadPreferenceUpdatableFieldsEquals(partialUpdatedLeadPreference, getPersistedLeadPreference(partialUpdatedLeadPreference));
    }

    @Test
    void patchNonExistingLeadPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadPreference.setId(longCount.incrementAndGet());

        // Create the LeadPreference
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(leadPreference);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, leadPreferenceDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchLeadPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadPreference.setId(longCount.incrementAndGet());

        // Create the LeadPreference
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(leadPreference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamLeadPreference() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadPreference.setId(longCount.incrementAndGet());

        // Create the LeadPreference
        LeadPreferenceDTO leadPreferenceDTO = leadPreferenceMapper.toDto(leadPreference);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadPreferenceDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the LeadPreference in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteLeadPreference() {
        // Initialize the database
        insertedLeadPreference = leadPreferenceRepository.save(leadPreference).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the leadPreference
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, leadPreference.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return leadPreferenceRepository.count().block();
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

    protected LeadPreference getPersistedLeadPreference(LeadPreference leadPreference) {
        return leadPreferenceRepository.findById(leadPreference.getId()).block();
    }

    protected void assertPersistedLeadPreferenceToMatchAllProperties(LeadPreference expectedLeadPreference) {
        // Test fails because reactive api returns an empty object instead of null
        // assertLeadPreferenceAllPropertiesEquals(expectedLeadPreference, getPersistedLeadPreference(expectedLeadPreference));
        assertLeadPreferenceUpdatableFieldsEquals(expectedLeadPreference, getPersistedLeadPreference(expectedLeadPreference));
    }

    protected void assertPersistedLeadPreferenceToMatchUpdatableProperties(LeadPreference expectedLeadPreference) {
        // Test fails because reactive api returns an empty object instead of null
        // assertLeadPreferenceAllUpdatablePropertiesEquals(expectedLeadPreference, getPersistedLeadPreference(expectedLeadPreference));
        assertLeadPreferenceUpdatableFieldsEquals(expectedLeadPreference, getPersistedLeadPreference(expectedLeadPreference));
    }
}
