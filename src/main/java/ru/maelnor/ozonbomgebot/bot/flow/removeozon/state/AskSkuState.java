package ru.maelnor.ozonbomgebot.bot.flow.removeozon.state;

import ru.maelnor.ozonbomgebot.bot.flow.*;
import ru.maelnor.ozonbomgebot.bot.flow.removeozon.RemoveOzonFlow;
import ru.maelnor.ozonbomgebot.bot.flow.util.Validators;

import static ru.maelnor.ozonbomgebot.bot.flow.util.FlowUtil.cb;

public final class AskSkuState implements State {

    @Override
    public Next enter(FlowContext ctx, FlowIO io) {
        var kb = FlowIO.Keyboard.builder().row(new FlowIO.Button("Отмена", cb(RemoveOzonFlow.FLOW_ID, "cancel"))).build();
        ctx.lastMessageId = io.send(ctx.chatId, "Введите артикул товара (SKU):", kb);
        return new Next.Stay();
    }

    @Override
    public Next onText(FlowContext ctx, FlowIO io, String text) {
        try {
            long sku = Validators.parseSku(text);
            ctx.put("sku", sku);
            return new Next.Done();
        } catch (IllegalArgumentException e) {
            io.edit(ctx.chatId, ctx.lastMessageId, e.getMessage(), null);
            return new Next.Stay();
        }
    }

    @Override
    public Next onCallback(FlowContext ctx, FlowIO io, String data) {
        if (data.equals(cb(RemoveOzonFlow.FLOW_ID, "cancel"))) return new Next.Cancel("пользовательская отмена");
        return new Next.Stay();
    }
}
