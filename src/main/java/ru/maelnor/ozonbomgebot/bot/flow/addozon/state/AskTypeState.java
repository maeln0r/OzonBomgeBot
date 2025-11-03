package ru.maelnor.ozonbomgebot.bot.flow.addozon.state;

import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.Next;
import ru.maelnor.ozonbomgebot.bot.flow.State;
import ru.maelnor.ozonbomgebot.bot.flow.addozon.AddOzonFlow;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;

import static ru.maelnor.ozonbomgebot.bot.flow.util.FlowUtil.cb;
import static ru.maelnor.ozonbomgebot.bot.flow.util.KeyboardUtil.backCancelKb;

public final class AskTypeState implements State {
    @Override
    public Next enter(FlowContext ctx, FlowIO io) {
        return new Next.Stay();
    }

    @Override
    public Next onText(FlowContext ctx, FlowIO io, String text) {
        return new Next.Stay();
    }

    @Override
    public Next onCallback(FlowContext ctx, FlowIO io, String data) {
        if (data.equals(cb(AddOzonFlow.FLOW_ID, "cancel"))) return new Next.Cancel("пользовательская отмена");
        if (data.equals(cb(AddOzonFlow.FLOW_ID, "type:PERCENT"))) {
            ctx.put("type", ThresholdType.PERCENT);
            io.edit(ctx.chatId, ctx.lastMessageId, "Введите процент (1–100):", backCancelKb(AddOzonFlow.FLOW_ID));
            return new Next.Goto("askValue");
        }
        if (data.equals(cb(AddOzonFlow.FLOW_ID, "type:PRICE"))) {
            ctx.put("type", ThresholdType.PRICE);
            io.edit(ctx.chatId, ctx.lastMessageId, "Введите целевую цену в рублях:", backCancelKb(AddOzonFlow.FLOW_ID));
            return new Next.Goto("askValue");
        }
        return new Next.Stay();
    }
}
