package ru.maelnor.ozonbomgebot.bot.flow;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemorySessionManager implements SessionManager {
    private static final long DEFAULT_TTL_SEC = 600;
    private final Map<String, Session> sessions = new HashMap<>();
    private final FlowIO io;

    public InMemorySessionManager(FlowIO io) {
        this.io = io;
    }

    private static String key(long chatId, long userId) {
        return chatId + ":" + userId;
    }

    @Override
    public Optional<Session> get(long chatId, long userId) {
        gc();
        return Optional.ofNullable(sessions.get(key(chatId, userId)));
    }

    @Override
    public Session start(String flowId, long chatId, long userId, Flow flow, long ttlSeconds) {
        gc();
        var s = new Session(flowId, flow, new FlowContext(chatId, userId, UUID.randomUUID(), Instant.now().plusSeconds(ttlSeconds)));
        sessions.put(key(chatId, userId), s);
        flow.start(s.ctx, io);
        return s;
    }

    @Override
    public Optional<FlowSignal> dispatch(Update update) {
        var chatId = Optional.ofNullable(update.getMessage() != null ? update.getMessage().getChatId() : null)
                .orElseGet(() -> update.getCallbackQuery() != null ? update.getCallbackQuery().getMessage().getChatId() : null);
        var userId = Optional.ofNullable(update.getMessage() != null ? update.getMessage().getFrom().getId() : null)
                .orElseGet(() -> update.getCallbackQuery() != null ? update.getCallbackQuery().getFrom().getId() : null);
        if (chatId == null || userId == null) return Optional.empty();

        var s = sessions.get(key(chatId, userId));
        if (s == null) return Optional.empty();
        s.ctx.expiresAt = Instant.now().plusSeconds(DEFAULT_TTL_SEC);
        return s.flow.handleUpdate(s.ctx, io, update);
    }

    @Override
    public void done(Session s) {
        sessions.remove(key(s.ctx.chatId, s.ctx.userId));
    }

    @Override
    public void cancel(Session s) {
        sessions.remove(key(s.ctx.chatId, s.ctx.userId));
    }

    private void gc() {
        var now = Instant.now();
        sessions.values().removeIf(sess -> sess.ctx.expiresAt.isBefore(now));
    }
}
