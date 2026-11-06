package io.github.strogolsky.profinder.messaging.mapper;

import io.github.strogolsky.profinder.messaging.dto.ChatMessageResponse;
import io.github.strogolsky.profinder.messaging.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper interface for converting {@link ChatMessage} entities
 * to {@link ChatMessageResponse} DTOs.
 * <p>
 * Uses MapStruct to generate the implementation automatically.
 * </p>
 */
@Mapper(componentModel = "jakarta")
public interface ChatMessageMapper {

    /**
     * Converts a single {@link ChatMessage} entity to a {@link ChatMessageResponse} DTO.
     *
     * @param entity the {@link ChatMessage} entity to convert
     * @return the resulting {@link ChatMessageResponse} DTO
     */
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "chatId", source = "chat.id")
    ChatMessageResponse toDto(ChatMessage entity);

    /**
     * Converts a list of {@link ChatMessage} entities to a list of {@link ChatMessageResponse} DTOs.
     *
     * @param entity list of chat messages to convert
     * @return list of mapped DTOs
     */
    List<ChatMessageResponse> toDtoList(List<ChatMessage> entity);
}
