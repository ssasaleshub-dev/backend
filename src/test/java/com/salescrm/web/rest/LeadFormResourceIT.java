package com.salescrm.web.rest;

import static com.salescrm.domain.LeadFormAsserts.*;
import static com.salescrm.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.IntegrationTest;
import com.salescrm.domain.LeadForm;
import com.salescrm.repository.EntityManager;
import com.salescrm.repository.LeadFormRepository;
import com.salescrm.service.dto.LeadFormDTO;
import com.salescrm.service.mapper.LeadFormMapper;
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
 * Integration tests for the {@link LeadFormResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class LeadFormResourceIT {

    private static final String DEFAULT_EXTERNAL_ID = "AAAAAAAAAA";
    private static final String UPDATED_EXTERNAL_ID = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/lead-forms";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LeadFormRepository leadFormRepository;

    @Autowired
    private LeadFormMapper leadFormMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private LeadForm leadForm;

    private LeadForm insertedLeadForm;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static LeadForm createEntity() {
        return new LeadForm().externalId(DEFAULT_EXTERNAL_ID).name(DEFAULT_NAME).status(DEFAULT_STATUS).createdAt(DEFAULT_CREATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static LeadForm createUpdatedEntity() {
        return new LeadForm().externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(LeadForm.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        leadForm = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedLeadForm != null) {
            leadFormRepository.delete(insertedLeadForm).block();
            insertedLeadForm = null;
        }
        deleteEntities(em);
    }

    @Test
    void createLeadForm() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the LeadForm
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);
        var returnedLeadFormDTO = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(LeadFormDTO.class)
            .returnResult()
            .getResponseBody();

        // Validate the LeadForm in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLeadForm = leadFormMapper.toEntity(returnedLeadFormDTO);
        assertLeadFormUpdatableFieldsEquals(returnedLeadForm, getPersistedLeadForm(returnedLeadForm));

        insertedLeadForm = returnedLeadForm;
    }

    @Test
    void createLeadFormWithExistingId() throws Exception {
        // Create the LeadForm with an existing ID
        leadForm.setId(1L);
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadForm in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkExternalIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        leadForm.setExternalId(null);

        // Create the LeadForm, which fails.
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        leadForm.setName(null);

        // Create the LeadForm, which fails.
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllLeadForms() {
        // Initialize the database
        insertedLeadForm = leadFormRepository.save(leadForm).block();

        // Get all the leadFormList
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
            .value(hasItem(leadForm.getId().intValue()))
            .jsonPath("$.[*].externalId")
            .value(hasItem(DEFAULT_EXTERNAL_ID))
            .jsonPath("$.[*].name")
            .value(hasItem(DEFAULT_NAME))
            .jsonPath("$.[*].status")
            .value(hasItem(DEFAULT_STATUS))
            .jsonPath("$.[*].createdAt")
            .value(hasItem(DEFAULT_CREATED_AT.toString()));
    }

    @Test
    void getLeadForm() {
        // Initialize the database
        insertedLeadForm = leadFormRepository.save(leadForm).block();

        // Get the leadForm
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, leadForm.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(leadForm.getId().intValue()))
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
    void getNonExistingLeadForm() {
        // Get the leadForm
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingLeadForm() throws Exception {
        // Initialize the database
        insertedLeadForm = leadFormRepository.save(leadForm).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the leadForm
        LeadForm updatedLeadForm = leadFormRepository.findById(leadForm.getId()).block();
        updatedLeadForm.externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(updatedLeadForm);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, leadFormDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the LeadForm in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLeadFormToMatchAllProperties(updatedLeadForm);
    }

    @Test
    void putNonExistingLeadForm() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadForm.setId(longCount.incrementAndGet());

        // Create the LeadForm
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, leadFormDTO.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadForm in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchLeadForm() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadForm.setId(longCount.incrementAndGet());

        // Create the LeadForm
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadForm in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamLeadForm() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadForm.setId(longCount.incrementAndGet());

        // Create the LeadForm
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the LeadForm in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateLeadFormWithPatch() throws Exception {
        // Initialize the database
        insertedLeadForm = leadFormRepository.save(leadForm).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the leadForm using partial update
        LeadForm partialUpdatedLeadForm = new LeadForm();
        partialUpdatedLeadForm.setId(leadForm.getId());

        partialUpdatedLeadForm.name(UPDATED_NAME).status(UPDATED_STATUS);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedLeadForm.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedLeadForm))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the LeadForm in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLeadFormUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedLeadForm, leadForm), getPersistedLeadForm(leadForm));
    }

    @Test
    void fullUpdateLeadFormWithPatch() throws Exception {
        // Initialize the database
        insertedLeadForm = leadFormRepository.save(leadForm).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the leadForm using partial update
        LeadForm partialUpdatedLeadForm = new LeadForm();
        partialUpdatedLeadForm.setId(leadForm.getId());

        partialUpdatedLeadForm.externalId(UPDATED_EXTERNAL_ID).name(UPDATED_NAME).status(UPDATED_STATUS).createdAt(UPDATED_CREATED_AT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedLeadForm.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedLeadForm))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the LeadForm in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLeadFormUpdatableFieldsEquals(partialUpdatedLeadForm, getPersistedLeadForm(partialUpdatedLeadForm));
    }

    @Test
    void patchNonExistingLeadForm() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadForm.setId(longCount.incrementAndGet());

        // Create the LeadForm
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, leadFormDTO.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadForm in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchLeadForm() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadForm.setId(longCount.incrementAndGet());

        // Create the LeadForm
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the LeadForm in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamLeadForm() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        leadForm.setId(longCount.incrementAndGet());

        // Create the LeadForm
        LeadFormDTO leadFormDTO = leadFormMapper.toDto(leadForm);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(leadFormDTO))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the LeadForm in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteLeadForm() {
        // Initialize the database
        insertedLeadForm = leadFormRepository.save(leadForm).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the leadForm
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, leadForm.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return leadFormRepository.count().block();
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

    protected LeadForm getPersistedLeadForm(LeadForm leadForm) {
        return leadFormRepository.findById(leadForm.getId()).block();
    }

    protected void assertPersistedLeadFormToMatchAllProperties(LeadForm expectedLeadForm) {
        // Test fails because reactive api returns an empty object instead of null
        // assertLeadFormAllPropertiesEquals(expectedLeadForm, getPersistedLeadForm(expectedLeadForm));
        assertLeadFormUpdatableFieldsEquals(expectedLeadForm, getPersistedLeadForm(expectedLeadForm));
    }

    protected void assertPersistedLeadFormToMatchUpdatableProperties(LeadForm expectedLeadForm) {
        // Test fails because reactive api returns an empty object instead of null
        // assertLeadFormAllUpdatablePropertiesEquals(expectedLeadForm, getPersistedLeadForm(expectedLeadForm));
        assertLeadFormUpdatableFieldsEquals(expectedLeadForm, getPersistedLeadForm(expectedLeadForm));
    }
}
