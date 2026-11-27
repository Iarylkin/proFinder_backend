package io.github.strogolsky.profinder.messaging.mapper;

import io.github.strogolsky.profinder.messaging.dto.ChatMessageResponse;
import io.github.strogolsky.profinder.messaging.dto.ChatResponse;
import io.github.strogolsky.profinder.messaging.entity.Chat;
import io.github.strogolsky.profinder.messaging.entity.ChatMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A chat is shown to each of its two users as a chat with the other one: the partner, with the first name read
 * from the core module. A message carries the ids of the chat and of the sender.
 */
class ChatMappersTest {

    private static final UUID CHAT_ID = UUID.randomUUID();
    private static final UUID ANNA = UUID.randomUUID();
    private static final UUID PETR = UUID.randomUUID();

    private final ChatMapper chatMapper = new ChatMapperImpl();
    private final ChatMessageMapper messageMapper = new ChatMessageMapperImpl();
    private final Function<UUID, String> firstNames = id -> Map.of(ANNA, "Anna", PETR, "Petr").get(id);

    private static Chat chat() {
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setUser1Id(ANNA);
        chat.setUser2Id(PETR);
        return chat;
    }

    @Test
    void firstUserSeesTheSecondOneAsThePartner() {
        ChatResponse dto = chatMapper.toDto(chat(), ANNA, firstNames);

        assertEquals(CHAT_ID, dto.chatId());
        assertEquals(PETR, dto.partnerId());
        assertEquals("Petr", dto.partnerFirstName());
    }

    @Test
    void secondUserSeesTheFirstOneAsThePartner() {
        ChatResponse dto = chatMapper.toDto(chat(), PETR, firstNames);

        assertEquals(ANNA, dto.partnerId());
        assertEquals("Anna", dto.partnerFirstName());
    }

    @Test
    void chatsAreConvertedAsAListForTheCurrentUser() {
        List<ChatResponse> dtos = chatMapper.toDtoList(List.of(chat(), chat()), ANNA, firstNames);

        assertEquals(2, dtos.size());
        assertEquals(PETR, dtos.get(0).partnerId());
    }

    @Test
    void partnerIsTheUserThatIsNotTheCurrentOne() {
        assertEquals(PETR, chatMapper.getPartnerId(chat(), ANNA));
        assertEquals(ANNA, chatMapper.getPartnerId(chat(), PETR));
    }

    @Test
    void messageCarriesTheIdsTheContentAndTheTime() {
        Chat chat = chat();
        ChatMessage message = new ChatMessage();
        UUID messageId = UUID.randomUUID();
        message.setId(messageId);
        message.setChat(chat);
        message.setSenderId(ANNA);
        message.setContent("Hello");
        LocalDateTime time = LocalDateTime.of(2026, 11, 27, 12, 0);
        message.setCreateAt(time);

        ChatMessageResponse dto = messageMapper.toDto(message);

        assertEquals(messageId, dto.id());
        assertEquals(CHAT_ID, dto.chatId());
        assertEquals(ANNA, dto.senderId());
        assertEquals("Hello", dto.content());
        assertEquals(time, dto.createAt());
    }

    @Test
    void messagesAreConvertedAsAList() {
        ChatMessage message = new ChatMessage();
        message.setChat(chat());

        assertEquals(1, messageMapper.toDtoList(List.of(message)).size());
    }
}
