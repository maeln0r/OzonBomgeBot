package ru.maelnor.ozonbomgebot.bot.flow.core;

import jakarta.annotation.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.maelnor.ozonbomgebot.bot.flow.*;
import ru.maelnor.ozonbomgebot.bot.flow.addozon.AddOzonFlow;
import ru.maelnor.ozonbomgebot.bot.flow.pricehistory.PriceHistoryFlow;
import ru.maelnor.ozonbomgebot.bot.flow.removeozon.RemoveOzonFlow;
import ru.maelnor.ozonbomgebot.bot.service.ChartStorageService;
import ru.maelnor.ozonbomgebot.bot.service.JobQueueService;
import ru.maelnor.ozonbomgebot.bot.service.PriceHistoryService;
import ru.maelnor.ozonbomgebot.bot.service.TrackedItemService;
import ru.maelnor.ozonbomgebot.bot.telegram.TelegramFlowIO;

@Configuration
public class BotFlowBridge {
    @Bean
    public FlowIO flowIO(TelegramClient telegramClient) {
        return new TelegramFlowIO(telegramClient);
    }

    @Bean
    public SessionManager sessionManager(FlowIO io) {
        return new InMemorySessionManager(io);
    }

    @Bean
    public FlowRegistry flowRegistry(TrackedItemService trackedItemService,
                                     PriceHistoryService priceHistoryService,
                                     @Nullable ChartStorageService chartStorageService,
                                     JobQueueService jobQueueService) {
        return new SimpleFlowRegistry()
                .register(AddOzonFlow.FLOW_ID, () -> new AddOzonFlow(trackedItemService, jobQueueService))
                .register(RemoveOzonFlow.FLOW_ID, () -> new RemoveOzonFlow(trackedItemService))
                .register(PriceHistoryFlow.FLOW_ID, () -> new PriceHistoryFlow(trackedItemService, priceHistoryService, chartStorageService));
    }
}