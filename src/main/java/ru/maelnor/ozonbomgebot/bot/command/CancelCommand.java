package ru.maelnor.ozonbomgebot.bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.Session;
import ru.maelnor.ozonbomgebot.bot.flow.SessionManager;

import java.util.Optional;

@Component
public class CancelCommand extends AbstractBotCommand {
    private final SessionManager sessions;

    public CancelCommand(FlowIO io, SessionManager sessions) {
        super(io, "/cancel", "Отменить текущий шаг", true);
        this.sessions = sessions;
    }

    @Override
    public void execute(Update update) {
        Long chatId = update.getMessage() != null ? update.getMessage().getChatId() : null;
        Long userId = update.getMessage() != null && update.getMessage().getFrom() != null ? update.getMessage().getFrom().getId() : null;
        if (chatId == null || userId == null) return;
        Optional<Session> session = sessions.get(chatId, userId);
        if (session.isPresent()) {
            sessions.cancel(session.get());
            io.edit(chatId, session.get().ctx.lastMessageId, "❌ Отменено: пользовательская отмена", null);
        } else {
            io.toast(chatId, "Не нашел активных сессий.");
        }
    }
}