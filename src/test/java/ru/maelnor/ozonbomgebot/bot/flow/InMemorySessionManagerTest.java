package ru.maelnor.ozonbomgebot.bot.flow;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InMemorySessionManagerTest {

    // простой flow-заглушка
    static class DummyFlow implements Flow {
        @Override
        public java.util.Optional<FlowSignal> start(FlowContext ctx, FlowIO io) {
            // имитируем, что flow сам пометил себя в контексте
            ctx.flowId = "dummy";
            ctx.currentStateId = "start";
            return Optional.of(FlowSignal.cont());
        }

        @Override
        public java.util.Optional<FlowSignal> handleUpdate(FlowContext ctx, FlowIO io, org.telegram.telegrambots.meta.api.objects.Update update) {
            return Optional.of(FlowSignal.done(ctx.chatId, ctx.lastMessageId != null ? ctx.lastMessageId : 0, "ok"));
        }
    }

    @Test
    void start_createsSessionAndStoresItByChatAndUser() {
        FlowIO io = mock(FlowIO.class);
        InMemorySessionManager sm = new InMemorySessionManager(io);

        Flow flow = new DummyFlow();
        Session session = sm.start("dummy", 100L, 200L, flow, 60);

        assertThat(session).isNotNull();
        assertThat(session.flowId).isEqualTo("dummy");
        assertThat(session.ctx.chatId).isEqualTo(100L);
        assertThat(session.ctx.userId).isEqualTo(200L);

        Optional<Session> fromGet = sm.get(100L, 200L);
        assertThat(fromGet).isPresent();
        assertThat(fromGet.get().flowId).isEqualTo("dummy");
    }

    @Test
    void done_removesSession() {
        FlowIO io = mock(FlowIO.class);
        InMemorySessionManager sm = new InMemorySessionManager(io);
        Session s = sm.start("dummy", 100L, 200L, new DummyFlow(), 60);

        sm.done(s);

        assertThat(sm.get(100L, 200L)).isEmpty();
    }

    @Test
    void cancel_removesSession() {
        FlowIO io = mock(FlowIO.class);
        InMemorySessionManager sm = new InMemorySessionManager(io);
        Session s = sm.start("dummy", 100L, 200L, new DummyFlow(), 60);

        sm.cancel(s);

        assertThat(sm.get(100L, 200L)).isEmpty();
    }
}