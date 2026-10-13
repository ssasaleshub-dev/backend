package com.salescrm.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.salescrm.domain.ChatMessage} entity.
 */
public class ChatMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String userLogin;

    @JsonAlias({ "sender", "senderName" })
    private String userName;

    private String userImageUrl;

    @NotNull
    @JsonAlias({ "message", "text" })
    private String content;

    private String channel = "general";

    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JsonProperty("sender")
    public String getSender() {
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        return userLogin;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public void setUserImageUrl(String userImageUrl) {
        this.userImageUrl = userImageUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @JsonProperty("message")
    public String getMessage() {
        return content;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        if (this.content == null || this.content.isBlank()) {
            this.content = message;
        }
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChatMessageDTO)) {
            return false;
        }
        ChatMessageDTO chatMessageDTO = (ChatMessageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, chatMessageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return "ChatMessageDTO{" +
            "id=" + getId() +
            ", userId=" + getUserId() +
            ", userLogin='" + getUserLogin() + "'" +
            ", userName='" + getUserName() + "'" +
            ", content='" + getContent() + "'" +
            ", channel='" + getChannel() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
