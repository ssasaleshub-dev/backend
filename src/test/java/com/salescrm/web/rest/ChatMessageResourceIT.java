package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.IntegrationTest;
import com.salescrm.domain.ChatMessage;
import com.salescrm.domain.User;
import com.salescrm.repository.ChatMessageRepository;
import com.salescrm.repository.EntityManager;
import com.salescrm.repository.UserRepository;
import com.salescrm.service.dto.ChatMessageDTO;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the {@link ChatMessageResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser(username = "admin")
class ChatMessageResourceIT {

    private static final String DEFAULT_CONTENT = "Hello team, let's close these leads!";
    private static final String DEFAULT_CHANNEL = "general";

    private static final String ENTITY_API_URL = "/api/messages";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Long defaultUserId;

    @BeforeEach
    void initTest() {
        chatMessageRepository.deleteAll().block();
        User testUser = userRepository.findOneByLogin("admin").block();
        if (testUser != null) {
            defaultUserId = testUser.getId();
        } else {
            User firstUser = userRepository.findAll().blockFirst();
            defaultUserId = firstUser != null ? firstUser.getId() : 1L;
        }
    }

    @AfterEach
    void cleanup() {
        chatMessageRepository.deleteAll().block();
    }

    @Test
    void createChatMessage() throws Exception {
        long databaseSizeBeforeCreate = chatMessageRepository.count().block();

        ChatMessageDTO chatMessageDTO = new ChatMessageDTO();
        chatMessageDTO.setContent(DEFAULT_CONTENT);
        chatMessageDTO.setChannel(DEFAULT_CHANNEL);
        chatMessageDTO.setUserId(defaultUserId);

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(chatMessageDTO))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody()
            .jsonPath("$.id").isNotEmpty()
            .jsonPath("$.content").isEqualTo(DEFAULT_CONTENT)
            .jsonPath("$.channel").isEqualTo(DEFAULT_CHANNEL)
            .jsonPath("$.userId").isEqualTo(defaultUserId.intValue());

        List<ChatMessage> chatMessageList = chatMessageRepository.findAll().collectList().block();
        assertThat(chatMessageList).hasSize((int) databaseSizeBeforeCreate + 1);
        ChatMessage testMessage = chatMessageList.get(chatMessageList.size() - 1);
        assertThat(testMessage.getContent()).isEqualTo(DEFAULT_CONTENT);
        assertThat(testMessage.getChannel()).isEqualTo(DEFAULT_CHANNEL);
        assertThat(testMessage.getUserId()).isEqualTo(defaultUserId);
    }

    @Test
    void getAllChatMessages() {
        ChatMessage message = new ChatMessage()
            .content(DEFAULT_CONTENT)
            .channel(DEFAULT_CHANNEL)
            .userId(defaultUserId)
            .createdAt(Instant.now());
        chatMessageRepository.save(message).block();

        webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id").isNotEmpty()
            .jsonPath("$.[*].content").value(org.hamcrest.Matchers.hasItem(DEFAULT_CONTENT))
            .jsonPath("$.[*].channel").value(org.hamcrest.Matchers.hasItem(DEFAULT_CHANNEL));
    }

    @Test
    void getChatMessagesByChannel() {
        ChatMessage message = new ChatMessage()
            .content("Sales discussion")
            .channel("leads")
            .userId(defaultUserId)
            .createdAt(Instant.now());
        chatMessageRepository.save(message).block();

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?channel=leads")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.[0].channel").isEqualTo("leads")
            .jsonPath("$.[0].content").isEqualTo("Sales discussion");
    }

    @Test
    void getChatMessageById() {
        ChatMessage message = new ChatMessage()
            .content(DEFAULT_CONTENT)
            .channel(DEFAULT_CHANNEL)
            .userId(defaultUserId)
            .createdAt(Instant.now());
        ChatMessage saved = chatMessageRepository.save(message).block();

        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, saved.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(saved.getId().intValue())
            .jsonPath("$.content").isEqualTo(DEFAULT_CONTENT);
    }

    @Test
    void deleteChatMessage() {
        ChatMessage message = new ChatMessage()
            .content(DEFAULT_CONTENT)
            .channel(DEFAULT_CHANNEL)
            .userId(defaultUserId)
            .createdAt(Instant.now());
        ChatMessage saved = chatMessageRepository.save(message).block();

        long databaseSizeBeforeDelete = chatMessageRepository.count().block();

        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, saved.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        List<ChatMessage> chatMessageList = chatMessageRepository.findAll().collectList().block();
        assertThat(chatMessageList).hasSize((int) databaseSizeBeforeDelete - 1);
    }
}
