package ru.maelnor.ozonbomgebot.bot.flow;

public interface State {
    Next enter(FlowContext ctx, FlowIO io);

    Next onText(FlowContext ctx, FlowIO io, String text);

    Next onCallback(FlowContext ctx, FlowIO io, String data);
}
