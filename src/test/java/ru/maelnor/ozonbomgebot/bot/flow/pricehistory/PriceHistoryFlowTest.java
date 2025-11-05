package ru.maelnor.ozonbomgebot.bot.flow.pricehistory;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.model.PricePoint;
import ru.maelnor.ozonbomgebot.bot.service.ChartStorageService;
import ru.maelnor.ozonbomgebot.bot.service.PriceChartBuilder;
import ru.maelnor.ozonbomgebot.bot.service.PriceHistoryService;
import ru.maelnor.ozonbomgebot.bot.service.TrackedItemService;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;


class PriceHistoryFlowTest {

    @Test
    void handleUpdate_whenSkuEntered_buildsChartAndDone() throws Exception {
        FlowIO io = mock(FlowIO.class);
        PriceHistoryService history = mock(PriceHistoryService.class);
        TrackedItemService trackedItemService = mock(TrackedItemService.class);
        PriceChartBuilder builder = mock(PriceChartBuilder.class);
        ChartStorageService  chartStorageService = mock(ChartStorageService.class);

        PriceHistoryFlow flow = new PriceHistoryFlow(trackedItemService, history,  chartStorageService);

        FlowContext ctx = new FlowContext(100L, 200L, UUID.randomUUID(), Instant.now().plusSeconds(600));
        ctx.flowId = "price_history";
        flow.start(ctx, io);

        // эмулируем ввод SKU
        Update u = new Update();
        Message m = new Message();
        m.setText("291003311");
        m.setChat(new Chat(100L, "private"));
        u.setMessage(m);

        when(history.getHistory(291003311L)).thenReturn(List.of(
                new PricePoint(5000L, 1710000000000L),
                new PricePoint(4800L, 1710003600000L)
        ));
        when(builder.buildChart(anyInt(), anyInt(), anyString())).thenReturn(new File("/tmp/fake.jpg"));

        var sigOpt = flow.handleUpdate(ctx, io, u);
    }
}
