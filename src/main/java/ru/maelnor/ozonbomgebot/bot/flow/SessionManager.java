package ru.maelnor.ozonbomgebot.bot.flow;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

public interface SessionManager {
    Optional<Session> get(long chatId, long userId);

    Session start(String flowId, long chatId, long userId, Flow flow, long ttlSeconds);

    Optional<FlowSignal> dispatch(Update update);

    void done(Session s);

    void cancel(Session s);
}
