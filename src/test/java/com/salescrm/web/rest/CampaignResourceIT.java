package com.salescrm.web.rest;

import static com.salescrm.domain.CampaignAsserts.*;
import static com.salescrm.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.IntegrationTest;
import com.salescrm.domain.Campaign;
import com.salescrm.repository.CampaignRepository;
import com.salescrm.repository.EntityManager;
import com.salescrm.service.dto.CampaignDTO;
import com.salescrm.service.mapper.CampaignMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the {@link CampaignResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class CampaignResourceIT {

    private static final String DEFAULT_EXTERNAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_EXTERNAL_ID = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/campaigns";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignMapper campaignMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Campaign campaign;

    private Campaign insertedCampaign;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Campaign createEntity() {
        return new Campaign()
            .externalId(DEFAULT_EXTERNAL_ID)
            .name(DEFAULT_NAME)
            .status(DEFAULT_STATUS)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Campaign createUpdatedEntity() {
        return new Campaign()
            .externalId(UPDATED_EXTERNAL_ID)
            .name(UPDATED_NAME)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Campaign.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        campaign = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCampaign != null) {
            campaignRepository.delete(insertedCampaign).block();
            insertedCampaign = null;
        }
        deleteEntities(em);
    }

    @Test
    void createCampaign() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);
        var returnedCampaignDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(CampaignDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the Campaign in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCampaign = campaignMapper.toEntity(returnedCampaignDTO);
        assertCampaignUpdatableFieldsEquals(returnedCampaign, getPersistedCampaign(returnedCampaign));

        insertedCampaign = returnedCampaign;
    }

    @Test
    void createCampaignWithExistingId() throws Exception {
        // Create the Campaign with an existing ID
        campaign.setId(1L);
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkExternalIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaign.setExternalId(null);

        // Create the Campaign, which fails.
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        campaign.setName(null);

        // Create the Campaign, which fails.
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllCampaigns() {
        // Initialize the database
        insertedCampaign = campaignRepository.save(campaign).block();

        // Get all the campaignList
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
            .value(hasItem(campaign.getId().intValue()))
            .jsonPath("$.[*].externalId")
            .value(hasItem(DEFAULT_EXTERNAL_ID))
            .jsonPath("$.[*].name")
            .value(hasItem(DEFAULT_NAME))
            .jsonPath("$.[*].status")
            .value(hasItem(DEFAULT_STATUS))
            .jsonPath("$.[*].createdAt")
            .value(hasItem(DEFAULT_CREATED_AT.toString()))
            .jsonPath("$.[*].updatedAt")
            .value(hasItem(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    void getCampaign() {
        // Initialize the database
        insertedCampaign = campaignRepository.save(campaign).block();

        // Get the campaign
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, campaign.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(campaign.getId().intValue()))
            .jsonPath("$.externalId")
            .value(is(DEFAULT_EXTERNAL_ID))
            .jsonPath("$.name")
            .value(is(DEFAULT_NAME))
            .jsonPath("$.status")
            .value(is(DEFAULT_STATUS))
            .jsonPath("$.createdAt")
            .value(is(DEFAULT_CREATED_AT.toString()))
            .jsonPath("$.updatedAt")
            .value(is(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    void getNonExistingCampaign() {
        // Get the campaign
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingCampaign() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.save(campaign).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaign
        Campaign updatedCampaign = campaignRepository.findById(campaign.getId()).block();
        updatedCampaign
            .externalId(UPDATED_EXTERNAL_ID)
            .name(UPDATED_NAME)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        CampaignDTO campaignDTO = campaignMapper.toDto(updatedCampaign);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, campaignDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCampaignToMatchAllProperties(updatedCampaign);
    }

    @Test
    void putNonExistingCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, campaignDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateCampaignWithPatch() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.save(campaign).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaign using partial update
        Campaign partialUpdatedCampaign = new Campaign();
        partialUpdatedCampaign.setId(campaign.getId());

        partialUpdatedCampaign.externalId(UPDATED_EXTERNAL_ID).createdAt(UPDATED_CREATED_AT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedCampaign.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedCampaign))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Campaign in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCampaignUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedCampaign, campaign), getPersistedCampaign(campaign));
    }

    @Test
    void fullUpdateCampaignWithPatch() throws Exception {
        // Initialize the database
        insertedCampaign = campaignRepository.save(campaign).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the campaign using partial update
        Campaign partialUpdatedCampaign = new Campaign();
        partialUpdatedCampaign.setId(campaign.getId());

        partialUpdatedCampaign
            .externalId(UPDATED_EXTERNAL_ID)
            .name(UPDATED_NAME)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedCampaign.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedCampaign))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Campaign in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCampaignUpdatableFieldsEquals(partialUpdatedCampaign, getPersistedCampaign(partialUpdatedCampaign));
    }

    @Test
    void patchNonExistingCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, campaignDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamCampaign() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        campaign.setId(longCount.incrementAndGet());

        // Create the Campaign
        CampaignDTO campaignDTO = campaignMapper.toDto(campaign);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(campaignDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Campaign in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteCampaign() {
        // Initialize the database
        insertedCampaign = campaignRepository.save(campaign).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the campaign
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, campaign.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return campaignRepository.count().block();
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

    protected Campaign getPersistedCampaign(Campaign campaign) {
        return campaignRepository.findById(campaign.getId()).block();
    }

    protected void assertPersistedCampaignToMatchAllProperties(Campaign expectedCampaign) {
        // Test fails because reactive api returns an empty object instead of null
        // assertCampaignAllPropertiesEquals(expectedCampaign, getPersistedCampaign(expectedCampaign));
        assertCampaignUpdatableFieldsEquals(expectedCampaign, getPersistedCampaign(expectedCampaign));
    }

    protected void assertPersistedCampaignToMatchUpdatableProperties(Campaign expectedCampaign) {
        // Test fails because reactive api returns an empty object instead of null
        // assertCampaignAllUpdatablePropertiesEquals(expectedCampaign, getPersistedCampaign(expectedCampaign));
        assertCampaignUpdatableFieldsEquals(expectedCampaign, getPersistedCampaign(expectedCampaign));
    }
}
