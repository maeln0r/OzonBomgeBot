package ru.maelnor.ozonbomgebot.bot.flow;

public final class Session {
    public final String flowId;
    public final Flow flow;
    public final FlowContext ctx;

    public Session(String flowId, Flow flow, FlowContext ctx) {
        this.flowId = flowId;
        this.flow = flow;
        this.ctx = ctx;
    }
}
