package ru.maelnor.ozonbomgebot.bot.command.ozon;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.maelnor.ozonbomgebot.bot.flow.*;
import ru.maelnor.ozonbomgebot.bot.flow.addozon.AddOzonFlow;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AddOzonFlowStartCommandTest {

    @Test
    void execute_whenNoActiveSession_startsFlow() {
        FlowIO io = mock(FlowIO.class);
        FlowRegistry registry = mock(FlowRegistry.class);
        SessionManager sessions = mock(SessionManager.class);

        when(sessions.get(1L, 2L)).thenReturn(Optional.empty());
        when(registry.create(AddOzonFlow.FLOW_ID)).thenReturn(Optional.of(mock(Flow.class)));

        AddOzonFlowStartCommand cmd = new AddOzonFlowStartCommand(io, registry, sessions);

        Update u = new Update();
        Message m = new Message();
        m.setChat(new Chat(1L, "private"));
        User user = User.builder().id(2L).firstName("Олег").isBot(false).build();
        m.setFrom(user);
        u.setMessage(m);

        cmd.execute(u);

        verify(sessions).start(eq(AddOzonFlow.FLOW_ID), eq(1L), eq(2L), any(Flow.class), eq(600L));
    }

    @Test
    void execute_whenSessionAlreadyExists_doesNothing() {
        FlowIO io = mock(FlowIO.class);
        FlowRegistry registry = mock(FlowRegistry.class);
        SessionManager sessions = mock(SessionManager.class);

        when(sessions.get(1L, 2L)).thenReturn(Optional.of(mock(Session.class)));

        AddOzonFlowStartCommand cmd = new AddOzonFlowStartCommand(io, registry, sessions);

        Update u = new Update();
        Message m = new Message();
        m.setChat(new Chat(1L, "private"));
        User user = User.builder().id(2L).firstName("Олег").isBot(false).build();
        m.setFrom(user);
        u.setMessage(m);

        cmd.execute(u);

        verify(sessions, never()).start(anyString(), anyLong(), anyLong(), any(), anyLong());
        verifyNoInteractions(registry);
    }
}
