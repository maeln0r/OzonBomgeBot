package ru.maelnor.ozonbomgebot.bot.flow.removeozon;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maelnor.ozonbomgebot.bot.flow.*;
import ru.maelnor.ozonbomgebot.bot.flow.removeozon.state.AskSkuState;
import ru.maelnor.ozonbomgebot.bot.service.TrackedItemService;

import java.util.Optional;

public final class RemoveOzonFlow implements Flow {
    public static final String FLOW_ID = "remove_ozon";
    private final TrackedItemService trackedItemService;

    public RemoveOzonFlow(TrackedItemService tis) {
        this.trackedItemService = tis;
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
                long chatId = ctx.chatId, sku = ctx.get("sku");

                var removed = trackedItemService.removeTracked(chatId, sku);
                String msg = "";
                if (removed.isPresent()) {
                    msg = "✅ Ок, удалил SKU " + sku + ".";
                } else {
                    msg = "❌ Товар с SKU " + sku + " не найден в списке.";
                }
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