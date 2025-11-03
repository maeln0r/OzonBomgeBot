package ru.maelnor.ozonbomgebot.bot.flow.signal;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.flow.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class DefaultFlowSignalHandlerTest {

    private FlowContext ctx() {
        FlowContext ctx = new FlowContext(100L, 200L, UUID.randomUUID(), Instant.now().plusSeconds(600));
        ctx.flowId = "any";
        return ctx;
    }

    @Test
    void handle_send_sendsAndStoresMessageId() {
        SessionManager sessions = mock(SessionManager.class);
        FlowIO io = mock(FlowIO.class);
        DefaultFlowSignalHandler h = new DefaultFlowSignalHandler(sessions);

        FlowContext ctx = ctx();
        when(io.send(100L, "hi", null)).thenReturn(42);

        h.handle(ctx, io, FlowSignal.send(100L, "hi"));

        verify(io).send(100L, "hi", null);
        // lastMessageId обновился
        assert ctx.lastMessageId == 42;
        // сессии не трогаем
        verifyNoInteractions(sessions);
    }

    @Test
    void handle_edit_whenNoLastMessage_sendsNew() {
        SessionManager sessions = mock(SessionManager.class);
        FlowIO io = mock(FlowIO.class);
        DefaultFlowSignalHandler h = new DefaultFlowSignalHandler(sessions);

        FlowContext ctx = ctx();
        when(io.send(100L, "text", null)).thenReturn(77);

        // edit без messageId и без lastMessageId → должно отправить новое
        h.handle(ctx, io, new FlowSignal.Edit(100L, null, "text", null));

        verify(io).send(100L, "text", null);
        assert ctx.lastMessageId == 77;
    }

    @Test
    void handle_done_editsAndMarksSessionDone() {
        SessionManager sessions = mock(SessionManager.class);
        FlowIO io = mock(FlowIO.class);
        DefaultFlowSignalHandler h = new DefaultFlowSignalHandler(sessions);

        FlowContext ctx = ctx();
        ctx.lastMessageId = 10;

        Session s = new Session("flow", mock(Flow.class), ctx);
        when(sessions.get(100L, 200L)).thenReturn(Optional.of(s));

        h.handle(ctx, io, new FlowSignal.Done(100L, null, "ok"));

        // редактнул
        verify(io).edit(100L, 10, "ok", null);
        // и завершил сессию
        verify(sessions).done(s);
    }

    @Test
    void handle_cancel_editsAndCancels() {
        SessionManager sessions = mock(SessionManager.class);
        FlowIO io = mock(FlowIO.class);
        DefaultFlowSignalHandler h = new DefaultFlowSignalHandler(sessions);

        FlowContext ctx = ctx();
        ctx.lastMessageId = 55;

        Session s = new Session("flow", mock(Flow.class), ctx);
        when(sessions.get(100L, 200L)).thenReturn(Optional.of(s));

        h.handle(ctx, io, new FlowSignal.Cancel(100L, null, "пользовательская отмена"));

        verify(io).edit(100L, 55, "❌ Отменено: пользовательская отмена", null);
        verify(sessions).cancel(s);
    }

    @Test
    void handle_continue_doesNothing() {
        SessionManager sessions = mock(SessionManager.class);
        FlowIO io = mock(FlowIO.class);
        DefaultFlowSignalHandler h = new DefaultFlowSignalHandler(sessions);

        h.handle(ctx(), io, FlowSignal.cont());

        verifyNoInteractions(io);
        verifyNoInteractions(sessions);
    }
}
