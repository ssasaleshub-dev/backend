package com.salescrm.web.rest;

import static com.salescrm.domain.LeadAsserts.*;
import static com.salescrm.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.IntegrationTest;
import com.salescrm.domain.Lead;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import com.salescrm.repository.EntityManager;
import com.salescrm.repository.LeadRepository;
import com.salescrm.service.LeadService;
import com.salescrm.service.dto.LeadDTO;
import com.salescrm.service.mapper.LeadMapper;
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
 * Integration tests for the {@link LeadResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class LeadResourceIT {

    private static final String DEFAULT_EXTERNAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_EXTERNAL_ID = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_TIME = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_TIME = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Boolean DEFAULT_IS_ORGANIC = false;
    private static final Boolean UPDATED_IS_ORGANIC = true;

    private static final PlatformType DEFAULT_PLATFORM = PlatformType.IG;
    private static final PlatformType UPDATED_PLATFORM = PlatformType.FB;

    private static final LeadStatus DEFAULT_STATUS = LeadStatus.CREATED;
    private static final LeadStatus UPDATED_STATUS = LeadStatus.CONTACTED;

    private static final String DEFAULT_NOTES = "AAAAAAAAAA";
    private static final String UPDATED_NOTES = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/leads";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LeadRepository leadRepository;

    @Mock
    private LeadRepository leadRepositoryMock;

    @Autowired
    private LeadMapper leadMapper;

    @Mock
    private LeadService leadServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Lead lead;

    private Lead insertedLead;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Lead createEntity() {
        return new Lead()
            .externalId(DEFAULT_EXTERNAL_ID)
            .createdTime(DEFAULT_CREATED_TIME)
            .isOrganic(DEFAULT_IS_ORGANIC)
            .platform(DEFAULT_PLATFORM)
            .status(DEFAULT_STATUS)
            .notes(DEFAULT_NOTES)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Lead createUpdatedEntity() {
        return new Lead()
            .externalId(UPDATED_EXTERNAL_ID)
            .createdTime(UPDATED_CREATED_TIME)
            .isOrganic(UPDATED_IS_ORGANIC)
            .platform(UPDATED_PLATFORM)
            .status(UPDATED_STATUS)
            .notes(UPDATED_NOTES)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Lead.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        lead = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedLead != null) {
            leadRepository.delete(insertedLead).block();
            insertedLead = null;
        }
        deleteEntities(em);
    }

    @Test
    void createLead() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Lead
        LeadDTO leadDTO = leadMapper.toDto(lead);
        var returnedLeadDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(LeadDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Lead in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLead = leadMapper.toEntity(returnedLeadDTO);
        assertLeadUpdatableFieldsEquals(returnedLead, getPersistedLead(returnedLead));

        insertedLead = returnedLead;
    }

    @Test
    void createLeadWithExistingId() throws Exception {
        // Create the Lead with an existing ID
        lead.setId(1L);
        LeadDTO leadDTO = leadMapper.toDto(lead);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Lead in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkExternalIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        lead.setExternalId(null);

        // Create the Lead, which fails.
        LeadDTO leadDTO = leadMapper.toDto(lead);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkCreatedTimeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        lead.setCreatedTime(null);

        // Create the Lead, which fails.
        LeadDTO leadDTO = leadMapper.toDto(lead);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkIsOrganicIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        lead.setIsOrganic(null);

        // Create the Lead, which fails.
        LeadDTO leadDTO = leadMapper.toDto(lead);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkPlatformIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        lead.setPlatform(null);

        // Create the Lead, which fails.
        LeadDTO leadDTO = leadMapper.toDto(lead);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkStatusIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        lead.setStatus(null);

        // Create the Lead, which fails.
        LeadDTO leadDTO = leadMapper.toDto(lead);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllLeads() {
        // Initialize the database
        insertedLead = leadRepository.save(lead).block();

        // Get all the leadList
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
            .value(hasItem(lead.getId().intValue()))
            .jsonPath("$.[*].externalId")
            .value(hasItem(DEFAULT_EXTERNAL_ID))
            .jsonPath("$.[*].createdTime")
            .value(hasItem(DEFAULT_CREATED_TIME.toString()))
            .jsonPath("$.[*].isOrganic")
            .value(hasItem(DEFAULT_IS_ORGANIC))
            .jsonPath("$.[*].platform")
            .value(hasItem(DEFAULT_PLATFORM.toString()))
            .jsonPath("$.[*].status")
            .value(hasItem(DEFAULT_STATUS.toString()))
            .jsonPath("$.[*].notes")
            .value(hasItem(DEFAULT_NOTES))
            .jsonPath("$.[*].createdAt")
            .value(hasItem(DEFAULT_CREATED_AT.toString()))
            .jsonPath("$.[*].updatedAt")
            .value(hasItem(DEFAULT_UPDATED_AT.toString()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLeadsWithEagerRelationshipsIsEnabled() {
        when(leadServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(leadServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLeadsWithEagerRelationshipsIsNotEnabled() {
        when(leadServiceMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(leadRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getLead() {
        // Initialize the database
        insertedLead = leadRepository.save(lead).block();

        // Get the lead
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, lead.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(lead.getId().intValue()))
            .jsonPath("$.externalId")
            .value(is(DEFAULT_EXTERNAL_ID))
            .jsonPath("$.createdTime")
            .value(is(DEFAULT_CREATED_TIME.toString()))
            .jsonPath("$.isOrganic")
            .value(is(DEFAULT_IS_ORGANIC))
            .jsonPath("$.platform")
            .value(is(DEFAULT_PLATFORM.toString()))
            .jsonPath("$.status")
            .value(is(DEFAULT_STATUS.toString()))
            .jsonPath("$.notes")
            .value(is(DEFAULT_NOTES))
            .jsonPath("$.createdAt")
            .value(is(DEFAULT_CREATED_AT.toString()))
            .jsonPath("$.updatedAt")
            .value(is(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    void getNonExistingLead() {
        // Get the lead
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingLead() throws Exception {
        // Initialize the database
        insertedLead = leadRepository.save(lead).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the lead
        Lead updatedLead = leadRepository.findById(lead.getId()).block();
        updatedLead
            .externalId(UPDATED_EXTERNAL_ID)
            .createdTime(UPDATED_CREATED_TIME)
            .isOrganic(UPDATED_IS_ORGANIC)
            .platform(UPDATED_PLATFORM)
            .status(UPDATED_STATUS)
            .notes(UPDATED_NOTES)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        LeadDTO leadDTO = leadMapper.toDto(updatedLead);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, leadDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Lead in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLeadToMatchAllProperties(updatedLead);
    }

    @Test
    void putNonExistingLead() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lead.setId(longCount.incrementAndGet());

        // Create the Lead
        LeadDTO leadDTO = leadMapper.toDto(lead);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, leadDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Lead in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchLead() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lead.setId(longCount.incrementAndGet());

        // Create the Lead
        LeadDTO leadDTO = leadMapper.toDto(lead);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Lead in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamLead() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lead.setId(longCount.incrementAndGet());

        // Create the Lead
        LeadDTO leadDTO = leadMapper.toDto(lead);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Lead in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateLeadWithPatch() throws Exception {
        // Initialize the database
        insertedLead = leadRepository.save(lead).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the lead using partial update
        Lead partialUpdatedLead = new Lead();
        partialUpdatedLead.setId(lead.getId());

        partialUpdatedLead.createdTime(UPDATED_CREATED_TIME).platform(UPDATED_PLATFORM).notes(UPDATED_NOTES);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedLead.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedLead))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Lead in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLeadUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedLead, lead), getPersistedLead(lead));
    }

    @Test
    void fullUpdateLeadWithPatch() throws Exception {
        // Initialize the database
        insertedLead = leadRepository.save(lead).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the lead using partial update
        Lead partialUpdatedLead = new Lead();
        partialUpdatedLead.setId(lead.getId());

        partialUpdatedLead
            .externalId(UPDATED_EXTERNAL_ID)
            .createdTime(UPDATED_CREATED_TIME)
            .isOrganic(UPDATED_IS_ORGANIC)
            .platform(UPDATED_PLATFORM)
            .status(UPDATED_STATUS)
            .notes(UPDATED_NOTES)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedLead.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedLead))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Lead in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLeadUpdatableFieldsEquals(partialUpdatedLead, getPersistedLead(partialUpdatedLead));
    }

    @Test
    void patchNonExistingLead() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lead.setId(longCount.incrementAndGet());

        // Create the Lead
        LeadDTO leadDTO = leadMapper.toDto(lead);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, leadDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Lead in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchLead() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lead.setId(longCount.incrementAndGet());

        // Create the Lead
        LeadDTO leadDTO = leadMapper.toDto(lead);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Lead in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamLead() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        lead.setId(longCount.incrementAndGet());

        // Create the Lead
        LeadDTO leadDTO = leadMapper.toDto(lead);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Lead in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteLead() {
        // Initialize the database
        insertedLead = leadRepository.save(lead).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the lead
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, lead.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return leadRepository.count().block();
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

    protected Lead getPersistedLead(Lead lead) {
        return leadRepository.findById(lead.getId()).block();
    }

    protected void assertPersistedLeadToMatchAllProperties(Lead expectedLead) {
        // Test fails because reactive api returns an empty object instead of null
        // assertLeadAllPropertiesEquals(expectedLead, getPersistedLead(expectedLead));
        assertLeadUpdatableFieldsEquals(expectedLead, getPersistedLead(expectedLead));
    }

    protected void assertPersistedLeadToMatchUpdatableProperties(Lead expectedLead) {
        // Test fails because reactive api returns an empty object instead of null
        // assertLeadAllUpdatablePropertiesEquals(expectedLead, getPersistedLead(expectedLead));
        assertLeadUpdatableFieldsEquals(expectedLead, getPersistedLead(expectedLead));
    }
}
