package ru.maelnor.ozonbomgebot.bot.handler;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.*;
import ru.maelnor.ozonbomgebot.bot.service.ChatCleanupService;

import static org.mockito.Mockito.*;

class ChatMemberHandlerTest {

    @Test
    void handle_whenBotKicked_callsCleanup() {
        ChatCleanupService cleanup = mock(ChatCleanupService.class);
        ChatMemberHandler h = new ChatMemberHandler(cleanup);

        Update u = new Update();
        ChatMemberUpdated mc = new ChatMemberUpdated();
        Chat chat = new Chat(999L, "group");
        mc.setChat(chat);

        ChatMember oldM = new ChatMemberMember();
        mc.setOldChatMember(oldM);

        ChatMember newM = new ChatMemberBanned();
        mc.setNewChatMember(newM);

        u.setMyChatMember(mc);

        h.handle(u);

        verify(cleanup).cleanupChat(999L, "group status member -> kicked");
    }

    @Test
    void handle_whenBotStillMember_doesNothing() {
        ChatCleanupService cleanup = mock(ChatCleanupService.class);
        ChatMemberHandler h = new ChatMemberHandler(cleanup);

        Update u = new Update();
        ChatMemberUpdated mc = new ChatMemberUpdated();
        Chat chat = new Chat(999L, "supergroup");
        mc.setChat(chat);

        ChatMember oldM = new ChatMemberMember();
        mc.setOldChatMember(oldM);

        ChatMember newM = new ChatMemberAdministrator();
        mc.setNewChatMember(newM);

        u.setMyChatMember(mc);

        h.handle(u);

        verifyNoInteractions(cleanup);
    }

    @Test
    void handle_whenNoMyChatMember_doNothing() {
        ChatCleanupService cleanup = mock(ChatCleanupService.class);
        ChatMemberHandler h = new ChatMemberHandler(cleanup);

        h.handle(new Update());

        verifyNoInteractions(cleanup);
    }
}
