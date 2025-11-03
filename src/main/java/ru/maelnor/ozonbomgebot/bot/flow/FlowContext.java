package ru.maelnor.ozonbomgebot.bot.flow;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FlowContext {
    public final long chatId;
    public final long userId;
    public final UUID sessionId;
    public String flowId;
    public Instant expiresAt;
    public String currentStateId;
    public Integer lastMessageId;
    private final Map<String, Object> bag = new HashMap<>();

    public FlowContext(long chatId, long userId, UUID sessionId, Instant ttlUntil) {
        this.chatId = chatId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.expiresAt = ttlUntil;
    }

    public <T> void put(String key, T value) {
        bag.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) bag.get(key);
    }
}
