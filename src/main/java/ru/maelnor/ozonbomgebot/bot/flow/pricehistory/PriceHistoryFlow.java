package ru.maelnor.ozonbomgebot.bot.flow.pricehistory;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maelnor.ozonbomgebot.bot.flow.*;
import ru.maelnor.ozonbomgebot.bot.flow.pricehistory.state.AskSkuState;
import ru.maelnor.ozonbomgebot.bot.model.PricePoint;
import ru.maelnor.ozonbomgebot.bot.service.ChartStorageService;
import ru.maelnor.ozonbomgebot.bot.service.PriceChartBuilder;
import ru.maelnor.ozonbomgebot.bot.service.PriceHistoryService;
import ru.maelnor.ozonbomgebot.bot.service.TrackedItemService;

import java.util.List;
import java.util.Optional;

public final class PriceHistoryFlow implements Flow {
    public static final String FLOW_ID = "price_history";
    private final TrackedItemService trackedItemService;
    private final PriceHistoryService priceHistoryService;
    private final ChartStorageService chartStorageService;


    public PriceHistoryFlow(TrackedItemService tis, PriceHistoryService priceHistoryService, ChartStorageService chartStorageService) {
        this.trackedItemService = tis;
        this.priceHistoryService = priceHistoryService;
        this.chartStorageService = chartStorageService;
    }

    @Override
    public Optional<FlowSignal> start(FlowContext ctx, FlowIO io) {
        ctx.currentStateId = "askSku";
        new AskSkuState().enter(ctx, io);
        return Optional.of(FlowSignal.cont());
    }

    @Override
    public Optional<FlowSignal> handleUpdate(FlowContext ctx, FlowIO io, Update u) {
        State state = switch (ctx.currentStateId) {
            case "askSku" -> new AskSkuState();
            default -> new AskSkuState();
        };

        Next next;
        if (u.hasCallbackQuery()) {
            next = state.onCallback(ctx, io, u.getCallbackQuery().getData());
        } else if (u.hasMessage() && u.getMessage().hasText()) {
            next = state.onText(ctx, io, u.getMessage().getText());
        } else {
            next = new Next.Stay();
        }

        switch (next) {
            case Next.Done done -> {
                long chatId = ctx.chatId;
                long sku = ctx.get("sku");

                // 1) тащим точки истории
                List<PricePoint> points = priceHistoryService.getHistory(sku);
                if (points == null || points.isEmpty()) {
                    return Optional.of(FlowSignal.done(chatId, ctx.lastMessageId,
                            "📈 По SKU " + sku + " пока нет истории цен."));
                }

                // 2) заголовок = либо название товара пользователя, либо просто SKU
                String title = trackedItemService.findTracked(chatId, sku)
                        .map(t -> t.getTitle() + " SKU(" + t.getSku() + ")")
                        .filter(t -> !t.isBlank())
                        .orElse("История цен • SKU " + sku);

                try {
                    // 3) берём самый свежий timestamp
                    long lastTs = points.stream()
                            .mapToLong(PricePoint::createdAtMs)
                            .max()
                            .orElse(points.getLast().createdAtMs());

                    java.io.File outFile;

                    // 4) если включён S3 и актуальный график уже лежит — скачиваем и шлём
                    if (chartStorageService.isEnabled() && chartStorageService.exists(sku, lastTs)) {
                        byte[] jpeg = chartStorageService.download(sku, lastTs);
                        outFile = java.io.File.createTempFile("price-" + sku + "-" + lastTs, ".jpg");
                        java.nio.file.Files.write(outFile.toPath(), jpeg);
                    } else {
                        // 5) строим локально
                        String tmpDir = System.getProperty("java.io.tmpdir");
                        outFile = java.nio.file.Path.of(tmpDir,
                                "price-" + sku + "-" + lastTs + ".jpg").toFile();

                        new PriceChartBuilder(points, title)
                                .buildChart(1280, 950, outFile.getAbsolutePath());

                        // 6) если S3 включён — загружаем готовый JPEG
                        if (chartStorageService.isEnabled()) {
                            byte[] jpeg = java.nio.file.Files.readAllBytes(outFile.toPath());
                            chartStorageService.upload(sku, lastTs, jpeg);
                        }
                    }

                    // 7) шлём фото
                    io.sendPhoto(chatId, outFile, "📈 " + title);
                    return Optional.of(FlowSignal.done(chatId, ctx.lastMessageId, "Готово ✅"));

                } catch (Exception e) {
                    return Optional.of(FlowSignal.done(chatId, ctx.lastMessageId,
                            "❌ Не удалось подготовить график: " + e.getMessage()));
                }
            }
            case Next.Cancel c -> {
                return Optional.of(FlowSignal.cancel(ctx.chatId, ctx.lastMessageId, c.reason()));
            }
            default -> {
                return Optional.of(FlowSignal.cont());
            }
        }
    }
}