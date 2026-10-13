package com.salescrm.service.mapper;

import com.salescrm.domain.ChatMessage;
import com.salescrm.service.dto.ChatMessageDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity {@link ChatMessage} and its DTO {@link ChatMessageDTO}.
 */
@Mapper(componentModel = "spring")
public interface ChatMessageMapper extends EntityMapper<ChatMessageDTO, ChatMessage> {}
