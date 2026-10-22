package com.salescrm.domain;

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A ChatMessage entity representing messages in the team chat.
 */
@Table("messages")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("user_id")
    private Long userId;

    @Column("content")
    private String content;

    @Column("channel")
    private String channel = "general";

    @Column("media_url")
    private String mediaUrl;

    @Column("media_type")
    private String mediaType; // "IMAGE", "VIDEO", "DOCUMENT", etc.

    @Column("file_name")
    private String fileName;

    @Column("file_size")
    private Long fileSize;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Transient
    private User user;

    public Long getId() {
        return this.id;
    }

    public ChatMessage id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public ChatMessage userId(Long userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return this.content;
    }

    public ChatMessage content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getChannel() {
        return this.channel;
    }

    public ChatMessage channel(String channel) {
        this.setChannel(channel);
        return this;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMediaUrl() {
        return this.mediaUrl;
    }

    public ChatMessage mediaUrl(String mediaUrl) {
        this.setMediaUrl(mediaUrl);
        return this;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public ChatMessage mediaType(String mediaType) {
        this.setMediaType(mediaType);
        return this;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getFileName() {
        return this.fileName;
    }

    public ChatMessage fileName(String fileName) {
        this.setFileName(fileName);
        return this;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return this.fileSize;
    }

    public ChatMessage fileSize(Long fileSize) {
        this.setFileSize(fileSize);
        return this;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public ChatMessage createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return this.user;
    }

    public ChatMessage user(User user) {
        this.setUser(user);
        return this;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChatMessage)) {
            return false;
        }
        return getId() != null && getId().equals(((ChatMessage) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
            "id=" + getId() +
            ", userId=" + getUserId() +
            ", content='" + getContent() + "'" +
            ", channel='" + getChannel() + "'" +
            ", mediaUrl='" + getMediaUrl() + "'" +
            ", mediaType='" + getMediaType() + "'" +
            ", fileName='" + getFileName() + "'" +
            ", fileSize=" + getFileSize() +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
