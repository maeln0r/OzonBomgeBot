package ru.maelnor.ozonbomgebot.bot.flow.signal;

import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.FlowSignal;

/**
 * Обработчик сигналов, умеющий сообщить, какие flow он поддерживает.
 */
public interface FlowSignalHandler {
    /** Возвращает true, если этот обработчик берет на себя обработку сигналов данного flowId. */
    boolean supports(String flowId);

    /** Выполнить обработку сигнала. */
    void handle(FlowContext ctx, FlowIO io, FlowSignal sig);
}