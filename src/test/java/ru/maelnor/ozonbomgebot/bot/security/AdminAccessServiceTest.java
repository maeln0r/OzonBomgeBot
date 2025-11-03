package ru.maelnor.ozonbomgebot.bot.security;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAccessServiceTest {

    private final TelegramClient telegramClient = mock(TelegramClient.class);
    private final AdminAccessService service = new AdminAccessService(telegramClient);

    @Test
    void isAdmin_privateChat_alwaysTrueAndNoTelegramCall() {
        Update upd = new Update();
        Message msg = new Message();
        Chat chat = new Chat(1L, "private");
        msg.setChat(chat);
        User user = new User(10L, "test", false);
        msg.setFrom(user);
        upd.setMessage(msg);

        boolean admin = service.isAdmin(upd);

        assertThat(admin).isTrue();
        verifyNoInteractions(telegramClient);
    }

    @Test
    void isAdmin_group_firstTimeCallsTelegram_secondTimeUsesCache() throws TelegramApiException {
        Update upd = new Update();
        Message msg = new Message();
        Chat chat = new Chat(100L, "group");
        msg.setChat(chat);
        User user = new User(10L, "test", false);
        msg.setFrom(user);
        upd.setMessage(msg);

        when(telegramClient.execute(any(GetChatMember.class)))
                .thenReturn(new ChatMemberAdministrator());

        boolean first = service.isAdmin(upd);
        boolean second = service.isAdmin(upd);

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        // к телеге обратились только 1 раз — дальше кэш
        verify(telegramClient, times(1)).execute(any(GetChatMember.class));
    }

    @Test
    void invalidateChat_clearsCacheForChat() throws TelegramApiException {
        // сначала положим что-то в кэш
        Update upd = new Update();
        Message msg = new Message();
        Chat chat = new Chat(200L, "supergroup");
        msg.setChat(chat);
        User user = new User(10L, "test", false);
        msg.setFrom(user);
        upd.setMessage(msg);

        when(telegramClient.execute(any(GetChatMember.class)))
                .thenReturn(new ChatMemberAdministrator());

        assertThat(service.isAdmin(upd)).isTrue();
        // инвалидируем
        service.invalidateChat(200L);

        // следующий вызов опять пойдёт в телегу
        assertThat(service.isAdmin(upd)).isTrue();
        verify(telegramClient, times(2)).execute(any(GetChatMember.class));
    }
}
