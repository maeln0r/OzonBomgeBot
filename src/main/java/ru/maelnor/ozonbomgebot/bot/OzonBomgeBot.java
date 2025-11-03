package ru.maelnor.ozonbomgebot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import ru.maelnor.ozonbomgebot.bot.command.BotCommand;
import ru.maelnor.ozonbomgebot.bot.command.CommandFactory;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.FlowSignal;
import ru.maelnor.ozonbomgebot.bot.flow.SessionManager;
import ru.maelnor.ozonbomgebot.bot.flow.signal.FlowSignalRouter;
import ru.maelnor.ozonbomgebot.bot.handler.ChatMemberHandler;
import ru.maelnor.ozonbomgebot.bot.security.AdminAccessService;

import java.util.Optional;

@Slf4j
@Component
public class OzonBomgeBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private final CommandFactory commandFactory;
    private final AdminAccessService adminAccess;
    private final ChatMemberHandler chatMemberHandler;
    private final SessionManager sessions;
    private final FlowSignalRouter signalRouter;
    private final FlowIO io;

    private final String botToken;

    public OzonBomgeBot(@Value("${app.bot.token}") String token,
                        CommandFactory commandFactory,
                        AdminAccessService adminAccess,
                        ChatMemberHandler chatMemberHandler,
                        SessionManager sessions,
                        FlowSignalRouter signalRouter,
                        FlowIO io) {
        this.commandFactory = commandFactory;
        this.signalRouter = signalRouter;
        this.io = io;
        this.botToken = token;
        this.adminAccess = adminAccess;
        this.chatMemberHandler = chatMemberHandler;
        this.sessions = sessions;
    }

    @Override
    public void consume(Update update) {
        // Хендлер для блокировки \ удаления бота из чата
        if (update.getMyChatMember() != null) {
            chatMemberHandler.handle(update);
            return;
        }

        Long chatId = null, userId = null;
        if (update.getMessage() != null) {
            chatId = update.getMessage().getChatId();
            userId = Optional.ofNullable(update.getMessage().getFrom()).map(User::getId).orElse(null);
        } else if (update.getCallbackQuery() != null) {
            chatId = Optional.ofNullable(update.getCallbackQuery().getMessage()).map(MaybeInaccessibleMessage::getChatId).orElse(null);
            userId = Optional.ofNullable(update.getCallbackQuery().getFrom()).map(User::getId).orElse(null);
        }
        if (chatId == null || userId == null) return;

        // Обработка основных команд
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Optional<BotCommand> command = commandFactory.getCommand(text);
            if (command.isPresent()) {
                if (command.get().isAdminOnly() && !adminAccess.isAdmin(update)) {
                    io.toast(chatId, "Недостаточно прав: команда доступна только администраторам чата.");
                } else {
                    command.get().execute(update);
                }
                return;
            }
        }

        // 2) Сессионные апдейты
        var optSession = sessions.get(chatId, userId);
        if (optSession.isEmpty()) return;

        var sigOpt = sessions.dispatch(update);
        if (sigOpt.isPresent()) {
            var sess = sessions.get(chatId, userId).orElse(null);
            if (sess != null) {
                FlowSignal sig = sigOpt.get();
                signalRouter.handle(sess.ctx, io, sig);
            }
        }
    }


    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }
}
