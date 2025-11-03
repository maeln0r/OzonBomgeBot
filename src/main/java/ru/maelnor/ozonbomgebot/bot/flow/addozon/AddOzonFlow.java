package ru.maelnor.ozonbomgebot.bot.flow.addozon;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maelnor.ozonbomgebot.bot.flow.*;
import ru.maelnor.ozonbomgebot.bot.flow.addozon.state.AskSkuState;
import ru.maelnor.ozonbomgebot.bot.flow.addozon.state.AskTypeState;
import ru.maelnor.ozonbomgebot.bot.flow.addozon.state.AskValueState;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;
import ru.maelnor.ozonbomgebot.bot.service.JobQueueService;
import ru.maelnor.ozonbomgebot.bot.service.TrackedItemService;

import java.util.Optional;

public final class AddOzonFlow implements Flow {
    public static final String FLOW_ID = "add_ozon";
    private final TrackedItemService trackedItemService;
    private final JobQueueService jobQueueService;

    public AddOzonFlow(TrackedItemService tis, JobQueueService jqs) {
        this.trackedItemService = tis;
        this.jobQueueService = jqs;
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
            case "askType" -> new AskTypeState();
            case "askValue" -> new AskValueState();
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
            case Next.Goto(String stateId) -> {
                ctx.currentStateId = stateId;
                switch (ctx.currentStateId) {
                    case "askType" -> new AskTypeState().enter(ctx, io);
                    case "askValue" -> new AskValueState().enter(ctx, io);
                    default -> new AskSkuState().enter(ctx, io);
                }
                return Optional.of(FlowSignal.cont());
            }
            case Next.Done done -> {
                long chatId = ctx.chatId, userId = ctx.userId, sku = ctx.get("sku");
                var type = (ThresholdType) ctx.get("type");

                trackedItemService.setThreshold(chatId, userId, sku, type, (type == ThresholdType.PERCENT) ? ctx.get("percent") : ctx.get("price"));
                jobQueueService.enqueueScanSku(sku);

                String msg = "✅ Добавлено. SKU: " + sku + ", порог: " + (
                        (ctx.get("type") == ThresholdType.PERCENT) ? (ctx.<Integer>get("percent") + "%") : (ctx.<Long>get("price") + "₽")
                ) + "\nПоставил в очередь на сканирование, результаты можно отслеживать в /list";

                return Optional.of(FlowSignal.done(chatId, ctx.lastMessageId, msg));
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