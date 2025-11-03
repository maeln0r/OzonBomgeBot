package ru.maelnor.ozonbomgebot.bot.flow.addozon.state;

import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.Next;
import ru.maelnor.ozonbomgebot.bot.flow.State;
import ru.maelnor.ozonbomgebot.bot.flow.addozon.AddOzonFlow;
import ru.maelnor.ozonbomgebot.bot.flow.util.Validators;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;

import static ru.maelnor.ozonbomgebot.bot.flow.util.FlowUtil.cb;

public final class AskValueState implements State {
    @Override
    public Next enter(FlowContext ctx, FlowIO io) {
        return new Next.Stay();
    }

    @Override
    public Next onText(FlowContext ctx, FlowIO io, String text) {
        var type = (ThresholdType) ctx.get("type");
        try {
            if (type == ThresholdType.PERCENT) {
                long p = Validators.parsePercent0to100(text);
                ctx.put("percent", p);
            } else {
                long price = Validators.parsePositiveMoneyRub(text);
                ctx.put("price", price);
            }
            return new Next.Done();
        } catch (IllegalArgumentException e) {
            io.toast(ctx.chatId, e.getMessage());
            return new Next.Stay();
        }
    }

    @Override
    public Next onCallback(FlowContext ctx, FlowIO io, String data) {
        if (data.equals(cb(AddOzonFlow.FLOW_ID, "cancel"))) return new Next.Cancel("пользовательская отмена");
        if (data.equals(cb(AddOzonFlow.FLOW_ID, "back"))) {
            io.edit(ctx.chatId, ctx.lastMessageId, "Артикул: " + ctx.<Long>get("sku") + "\nВыберите порог срабатывания:", AskSkuState.typeKb());
            return new Next.Goto("askType");
        }
        return new Next.Stay();
    }
}
