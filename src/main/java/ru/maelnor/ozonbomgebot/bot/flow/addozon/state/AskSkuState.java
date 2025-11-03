package ru.maelnor.ozonbomgebot.bot.flow.addozon.state;

import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.Next;
import ru.maelnor.ozonbomgebot.bot.flow.State;
import ru.maelnor.ozonbomgebot.bot.flow.addozon.AddOzonFlow;
import ru.maelnor.ozonbomgebot.bot.flow.util.Validators;

import static ru.maelnor.ozonbomgebot.bot.flow.util.FlowUtil.cb;

public final class AskSkuState implements State {

    @Override
    public Next enter(FlowContext ctx, FlowIO io) {
        var kb = FlowIO.Keyboard.builder().row(new FlowIO.Button("Отмена", cb(AddOzonFlow.FLOW_ID, "cancel"))).build();
        ctx.lastMessageId = io.send(ctx.chatId, "Введите артикул товара (SKU):", kb);
        return new Next.Stay();
    }

    @Override
    public Next onText(FlowContext ctx, FlowIO io, String text) {
        try {
            long sku = Validators.parseSku(text);
            ctx.put("sku", sku);
            io.edit(ctx.chatId, ctx.lastMessageId, "Артикул: " + sku + "\nВыберите порог срабатывания:", typeKb());
            return new Next.Goto("askType");
        } catch (IllegalArgumentException e) {
            io.toast(ctx.chatId, e.getMessage());
            return new Next.Stay();
        }
    }

    @Override
    public Next onCallback(FlowContext ctx, FlowIO io, String data) {
        if (data.equals(cb(AddOzonFlow.FLOW_ID, "cancel"))) return new Next.Cancel("пользовательская отмена");
        return new Next.Stay();
    }

    static FlowIO.Keyboard typeKb() {
        return FlowIO.Keyboard.builder()
                .row(new FlowIO.Button("Проценты", cb(AddOzonFlow.FLOW_ID, "type:PERCENT")), new FlowIO.Button("Цена", cb(AddOzonFlow.FLOW_ID, "type:PRICE")))
                .row(new FlowIO.Button("Отмена", cb(AddOzonFlow.FLOW_ID, "cancel"))).build();
    }
}
