package ru.maelnor.ozonbomgebot.bot;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.maelnor.ozonbomgebot.bot.command.BotCommand;
import ru.maelnor.ozonbomgebot.bot.command.CommandFactory;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.FlowSignal;
import ru.maelnor.ozonbomgebot.bot.flow.Session;
import ru.maelnor.ozonbomgebot.bot.flow.SessionManager;
import ru.maelnor.ozonbomgebot.bot.flow.signal.FlowSignalRouter;
import ru.maelnor.ozonbomgebot.bot.handler.ChatMemberHandler;
import ru.maelnor.ozonbomgebot.bot.security.AdminAccessService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OzonBomgeBotTest {

    private OzonBomgeBot bot(CommandFactory cf,
                             AdminAccessService admin,
                             ChatMemberHandler cm,
                             SessionManager sessions,
                             FlowSignalRouter router,
                             FlowIO io) {
        return new OzonBomgeBot("TOKEN", cf, admin, cm, sessions, router, io);
    }

    @Test
    void consume_myChatMember_goesToHandlerOnly() {
        CommandFactory cf = mock(CommandFactory.class);
        AdminAccessService admin = mock(AdminAccessService.class);
        ChatMemberHandler cm = mock(ChatMemberHandler.class);
        SessionManager sessions = mock(SessionManager.class);
        FlowSignalRouter router = mock(FlowSignalRouter.class);
        FlowIO io = mock(FlowIO.class);

        OzonBomgeBot bot = bot(cf, admin, cm, sessions, router, io);

        Update u = new Update();
        u.setMyChatMember(new ChatMemberUpdated());

        bot.consume(u);

        verify(cm).handle(u);
        verifyNoInteractions(cf, sessions, router, io);
    }

    @Test
    void consume_messageWithCommand_executesCommand() {
        CommandFactory cf = mock(CommandFactory.class);
        AdminAccessService admin = mock(AdminAccessService.class);
        ChatMemberHandler cm = mock(ChatMemberHandler.class);
        SessionManager sessions = mock(SessionManager.class);
        FlowSignalRouter router = mock(FlowSignalRouter.class);
        FlowIO io = mock(FlowIO.class);

        BotCommand cmd = mock(BotCommand.class);
        when(cmd.isAdminOnly()).thenReturn(false);
        when(cf.getCommand("/list")).thenReturn(Optional.of(cmd));

        OzonBomgeBot bot = bot(cf, admin, cm, sessions, router, io);

        Update u = new Update();
        Message m = new Message();
        m.setText("/list");
        m.setChat(new Chat(100L, "private"));
        User from = User.builder().id(200L).firstName("Олег").isBot(false).build();
        m.setFrom(from);
        u.setMessage(m);

        bot.consume(u);

        verify(cmd).execute(u);
        verifyNoInteractions(sessions, router);
    }

    @Test
    void consume_whenSessionExists_routesSignal() {
        CommandFactory cf = mock(CommandFactory.class);
        AdminAccessService admin = mock(AdminAccessService.class);
        ChatMemberHandler cm = mock(ChatMemberHandler.class);
        SessionManager sessions = mock(SessionManager.class);
        FlowSignalRouter router = mock(FlowSignalRouter.class);
        FlowIO io = mock(FlowIO.class);

        OzonBomgeBot bot = bot(cf, admin, cm, sessions, router, io);

        // апдейт без команды
        Update u = new Update();
        Message m = new Message();
        m.setText("some text");
        m.setChat(new Chat(100L, "private"));
        User from = User.builder().id(200L).firstName("Олег").isBot(false).build();
        m.setFrom(from);
        u.setMessage(m);

        // есть активная сессия
        Session s = new Session("remove_ozon", mock(ru.maelnor.ozonbomgebot.bot.flow.Flow.class),
                new ru.maelnor.ozonbomgebot.bot.flow.FlowContext(100L, 200L,
                        java.util.UUID.randomUUID(), java.time.Instant.now().plusSeconds(600)));
        when(sessions.get(100L, 200L)).thenReturn(Optional.of(s));
        when(sessions.dispatch(u)).thenReturn(Optional.of(FlowSignal.toast(100L, "hi")));

        bot.consume(u);

        verify(router).handle(eq(s.ctx), eq(io), any(FlowSignal.class));
        verifyNoInteractions(cm);
    }
}
