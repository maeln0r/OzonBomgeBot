package ru.maelnor.ozonbomgebot.bot.command.ozon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.maelnor.ozonbomgebot.bot.command.AbstractBotCommand;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.FlowRegistry;
import ru.maelnor.ozonbomgebot.bot.flow.SessionManager;
import ru.maelnor.ozonbomgebot.bot.flow.pricehistory.PriceHistoryFlow;

import java.util.Optional;

@Slf4j
@Component
public class PriceHistoryFlowStartCommand extends AbstractBotCommand {
    private final FlowRegistry flowRegistry;
    private final SessionManager sessions;

    public PriceHistoryFlowStartCommand(FlowIO io, FlowRegistry registry, SessionManager sessions) {
        super(io, "/price_history", "История изменения цен", false);
        this.flowRegistry = registry;
        this.sessions = sessions;
    }

    @Override
    public void execute(Update update) {
        if (update.getMessage() == null) return;
        long chatId = update.getMessage().getChatId();
        long userId = Optional.ofNullable(update.getMessage().getFrom()).map(User::getId).orElse(0L);

        if (sessions.get(chatId, userId).isPresent()) return;

        var flow = flowRegistry.create(PriceHistoryFlow.FLOW_ID)
                .orElseThrow(() -> new IllegalStateException("Flow not registered: " + PriceHistoryFlow.FLOW_ID));
        sessions.start(PriceHistoryFlow.FLOW_ID, chatId, userId, flow, 600);
    }
}