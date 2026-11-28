package io.github.strogolsky.profinder.messaging.repository;

import io.github.strogolsky.profinder.messaging.entity.Chat;
import io.github.strogolsky.profinder.messaging.entity.ChatMessage;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The queries of the chat repositories, on a real PostgreSQL database of the {@code messaging} module.
 * A chat belongs to a pair of users that are only ids, kept in order: the smaller id is always the first.
 */
@QuarkusTest
class MessagingRepositoriesTest {

    private static final int PAGE_SIZE = 2;

    @Inject
    ChatRepository chats;

    @Inject
    ChatMessageRepository messages;

    private static UUID[] pair() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        return a.compareTo(b) <= 0 ? new UUID[] {a, b} : new UUID[] {b, a};
    }

    private Chat chat(final UUID first, final UUID second) {
        Chat chat = new Chat();
        chat.setUser1Id(first);
        chat.setUser2Id(second);
        chats.persist(chat);
        return chat;
    }

    private ChatMessage message(final Chat chat, final UUID sender, final String content,
                                final LocalDateTime time) {
        ChatMessage message = new ChatMessage();
        message.setChat(chat);
        message.setSenderId(sender);
        message.setContent(content);
        message.setCreateAt(time);
        messages.persist(message);
        return message;
    }

    // ---- ChatRepository ----

    @Test
    @TestTransaction
    void chatIsFoundByItsPairOfUsersInAnyOrder() {
        UUID[] users = pair();
        Chat saved = chat(users[0], users[1]);

        assertEquals(saved.getId(), chats.findByUsersId(users[0], users[1]).getId());
        assertEquals(saved.getId(), chats.findByUsersId(users[1], users[0]).getId());
    }

    @Test
    @TestTransaction
    void unknownPairHasNoChat() {
        UUID[] users = pair();

        assertNull(chats.findByUsersId(users[0], users[1]));
    }

    @Test
    @TestTransaction
    void chatsOfAUserAreTheOnesWhereHeIsOnEitherSide() {
        UUID me = UUID.randomUUID();
        Chat asFirst = chat(me, UUID.randomUUID());
        Chat asSecond = chat(UUID.randomUUID(), me);
        chat(UUID.randomUUID(), UUID.randomUUID());

        Set<UUID> found = chats.findByUserId(me, 1, 10).stream().map(Chat::getId).collect(Collectors.toSet());

        assertEquals(Set.of(asFirst.getId(), asSecond.getId()), found);
    }

    @Test
    @TestTransaction
    void chatsOfAUserAreReadByPageWhichStartsAtOne() {
        UUID me = UUID.randomUUID();
        chat(me, UUID.randomUUID());
        chat(me, UUID.randomUUID());
        chat(me, UUID.randomUUID());

        List<Chat> firstPage = chats.findByUserId(me, 1, PAGE_SIZE);
        List<Chat> secondPage = chats.findByUserId(me, 2, PAGE_SIZE);

        assertEquals(2, firstPage.size());
        assertEquals(1, secondPage.size());
    }

    @Test
    @TestTransaction
    void theSamePairCannotHaveTwoChats() {
        UUID[] users = pair();
        chat(users[0], users[1]);

        assertThrows(PersistenceException.class, () -> {
            chat(users[0], users[1]);
            chats.getEntityManager().flush();
        });
    }

    // ---- ChatMessageRepository ----

    @Test
    @TestTransaction
    void messagesAreNewestFirst() {
        UUID[] users = pair();
        Chat chat = chat(users[0], users[1]);
        LocalDateTime noon = LocalDateTime.of(2026, 11, 28, 12, 0);
        message(chat, users[0], "second", noon.plusMinutes(1));
        message(chat, users[1], "first", noon);
        message(chat, users[0], "third", noon.plusMinutes(2));

        List<String> contents = messages.findByChatId(chat.getId(), 1, 10).stream()
                .map(ChatMessage::getContent).toList();

        assertEquals(List.of("third", "second", "first"), contents);
    }

    @Test
    @TestTransaction
    void messagesOfAChatDoNotIncludeThoseOfAnotherChat() {
        UUID[] users = pair();
        Chat mine = chat(users[0], users[1]);
        Chat other = chat(UUID.randomUUID(), UUID.randomUUID());
        message(mine, users[0], "mine", LocalDateTime.of(2026, 11, 28, 12, 0));
        message(other, users[0], "not mine", LocalDateTime.of(2026, 11, 28, 12, 1));

        List<ChatMessage> found = messages.findByChatId(mine.getId(), 1, 10);

        assertEquals(1, found.size());
        assertEquals("mine", found.get(0).getContent());
    }

    @Test
    @TestTransaction
    void messagesAreReadByPageWhichStartsAtOne() {
        UUID[] users = pair();
        Chat chat = chat(users[0], users[1]);
        LocalDateTime noon = LocalDateTime.of(2026, 11, 28, 12, 0);
        message(chat, users[0], "a", noon);
        message(chat, users[0], "b", noon.plusMinutes(1));
        message(chat, users[0], "c", noon.plusMinutes(2));

        assertEquals(List.of("c", "b"),
                messages.findByChatId(chat.getId(), 1, PAGE_SIZE).stream().map(ChatMessage::getContent).toList());
        assertEquals(List.of("a"),
                messages.findByChatId(chat.getId(), 2, PAGE_SIZE).stream().map(ChatMessage::getContent).toList());
    }

    @Test
    @TestTransaction
    void deletingAChatDeletesItsMessagesToo() {
        UUID[] users = pair();
        Chat chat = chat(users[0], users[1]);
        UUID chatId = chat.getId();
        message(chat, users[0], "bye", LocalDateTime.of(2026, 11, 28, 12, 0));
        chats.getEntityManager().flush();
        chats.getEntityManager().clear();

        chats.delete(chats.findById(chatId));
        chats.getEntityManager().flush();
        chats.getEntityManager().clear();

        assertTrue(messages.findByChatId(chatId, 1, 10).isEmpty());
    }
}
