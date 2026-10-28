package fit.biejk.mapper;

import fit.biejk.dto.ChatResponse;
import fit.biejk.entity.Chat;
import fit.biejk.entity.User;
import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

/**
 * {@code ChatMapper} converts {@link Chat} entities into {@link ChatResponse} DTOs using MapStruct.
 * <p>
 * Each {@link ChatResponse} includes:
 * <ul>
 *     <li>{@code chatId} – the identifier of the chat.</li>
 *     <li>{@code partnerId} – the identifier of the chat partner (computed based on the current user).</li>
 *     <li>{@code partnerFirstName} – first name of the chat partner.</li>
 * </ul>
 * <p>
 * The mapper requires the {@code currentUserId} in {@link org.mapstruct.Context} to correctly resolve the
 * partner in the chat.
 * <p>
 * <b>Thread‑safety:</b> This mapper is stateless and therefore thread‑safe.
 */
@Mapper(componentModel = "jakarta")
public interface ChatMapper {

    /**
     * Converts a {@link Chat} entity to its DTO representation.
     *
     * @param chat          chat entity to convert
     * @param currentUserId id of the user making the request, needed to identify the chat partner
     * @return populated {@link ChatResponse}
     */
    @Mapping(target = "chatId", source = "id")
    @Mapping(target = "partnerId",
            expression = "java(getPartner(chat, currentUserId).getId())")
    @Mapping(target = "partnerFirstName",
            expression = "java(getPartner(chat, currentUserId).getFirstName())")
    ChatResponse toDto(Chat chat, @Context UUID currentUserId);

    /**
     * Converts a list of {@link Chat} entities to DTOs.
     *
     * @param chats         list of chat entities
     * @param currentUserId id of the user making the request
     * @return list of {@link ChatResponse} DTOs
     */
    @IterableMapping(elementTargetType = ChatResponse.class)
    List<ChatResponse> toDtoList(List<Chat> chats, @Context UUID currentUserId);

    /**
     * Returns the partner of the current user in the given chat.
     *
     * @param chat          the chat entity
     * @param currentUserId id of the current user
     * @return the partner {@link User} entity
     */
    default User getPartner(final Chat chat, final UUID currentUserId) {
        return chat.getUser1().getId().equals(currentUserId)
                ? chat.getUser2()
                : chat.getUser1();
    }
}
