package ru.maelnor.ozonbomgebot.bot.flow;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

public interface Flow {
    Optional<FlowSignal> start(FlowContext ctx, FlowIO io);

    Optional<FlowSignal> handleUpdate(FlowContext ctx, FlowIO io, Update update);
}