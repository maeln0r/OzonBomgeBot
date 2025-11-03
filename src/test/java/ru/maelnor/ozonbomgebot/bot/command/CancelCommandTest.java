package ru.maelnor.ozonbomgebot.bot.command;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.maelnor.ozonbomgebot.bot.flow.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class CancelCommandTest {

    @Test
    void execute_whenSessionExists_cancelsAndEdits() {
        FlowIO io = mock(FlowIO.class);
        SessionManager sessions = mock(SessionManager.class);

        FlowContext ctx = new FlowContext(100L, 200L, UUID.randomUUID(), Instant.now().plusSeconds(600));
        ctx.lastMessageId = 55;

        Session sess = new Session("remove_ozon", mock(Flow.class), ctx);

        when(sessions.get(100L, 200L)).thenReturn(Optional.of(sess));

        CancelCommand cmd = new CancelCommand(io, sessions);

        Update u = new Update();
        Message m = new Message();
        m.setChat(new Chat(100L, "private"));

        User user = User.builder().id(200L).firstName("Олег").isBot(false).build();
        m.setFrom(user);

        u.setMessage(m);

        cmd.execute(u);

        verify(sessions).cancel(sess);
        verify(io).edit(100L, 55, "❌ Отменено: пользовательская отмена", null);
    }

    @Test
    void execute_whenNoSession_toast() {
        FlowIO io = mock(FlowIO.class);
        SessionManager sessions = mock(SessionManager.class);

        when(sessions.get(100L, 200L)).thenReturn(Optional.empty());

        CancelCommand cmd = new CancelCommand(io, sessions);

        Update u = new Update();
        Message m = new Message();
        m.setChat(new Chat(100L, "private"));

        User user = User.builder().id(200L).firstName("Олег").isBot(false).build();
        m.setFrom(user);

        u.setMessage(m);

        cmd.execute(u);

        verify(io).toast(100L, "Не нашел активных сессий.");
        verify(sessions, never()).cancel(any());
    }
}
