package ru.maelnor.ozonbomgebot.bot.flow.signal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.FlowSignal;
import ru.maelnor.ozonbomgebot.bot.flow.SessionManager;

/**
 * Дефолтная реализация: SEND/EDIT/CONTINUE/FINISH/CANCEL.
 * Ничего не знает о конкретных flow – только универсальная логика.
 */
@Component
@RequiredArgsConstructor
public class DefaultFlowSignalHandler implements FlowSignalHandler {
    private final SessionManager sessions;

    @Override
    public boolean supports(String flowId) {
        return true;
    }

    @Override
    public void handle(FlowContext ctx, FlowIO io, FlowSignal sig) {
        switch (sig) {
            case FlowSignal.Send s -> {
                // отправляем новое сообщение и запоминаем его id
                ctx.lastMessageId = io.send(ctx.chatId, s.text(), s.keyboard());
            }
            case FlowSignal.Edit e -> {
                // редактируем либо указанный messageId, либо последний из контекста
                Integer targetMid = (e.messageId() != null) ? e.messageId() : ctx.lastMessageId;
                if (targetMid != null) {
                    io.edit(ctx.chatId, targetMid, e.text(), e.keyboard());
                } else {
                    // если нечего редактировать - отправим новое
                    ctx.lastMessageId = io.send(ctx.chatId, e.text(), e.keyboard());
                }
            }
            case FlowSignal.Toast t -> {
                // короткое однократное сообщение
                io.toast(ctx.chatId, t.text());
            }
            case FlowSignal.Continue cont -> {
            }
            case FlowSignal.Done d -> {
                Integer targetMid = (d.messageId() != null) ? d.messageId() : ctx.lastMessageId;
                if (targetMid != null) {
                    io.edit(ctx.chatId, targetMid, d.text(), null);
                } else {
                    ctx.lastMessageId = io.send(ctx.chatId, d.text(), null);
                }
                // успешное завершение сессии
                sessions.get(ctx.chatId, ctx.userId).ifPresent(sessions::done);
            }
            case FlowSignal.Cancel c -> {
                // уведомление об отмене
                if (ctx.lastMessageId != null) {
                    io.edit(ctx.chatId, ctx.lastMessageId, "❌ Отменено: " + c.reason(), null);
                } else {
                    io.toast(ctx.chatId, "❌ Отменено: " + c.reason());
                }
                sessions.get(ctx.chatId, ctx.userId).ifPresent(sessions::cancel);
            }
            case FlowSignal.SendPhoto p -> {
                io.sendPhoto(p.chatId(), p.file(), p.caption());
            }

            default -> throw new IllegalStateException("Unexpected signal: " + sig);
        }
    }
}