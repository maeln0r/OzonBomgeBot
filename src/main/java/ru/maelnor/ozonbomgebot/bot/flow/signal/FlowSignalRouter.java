package ru.maelnor.ozonbomgebot.bot.flow.signal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.FlowSignal;

import java.util.List;

/**
 * Роутер: выбирает первый handler, который поддерживает текущий flowId.
 * Если специализированного нет – применяет дефолтный.
 */
@Component
@RequiredArgsConstructor
public class FlowSignalRouter implements FlowSignalHandler {
    private final List<FlowSignalHandler> handlers;

    @Override
    public boolean supports(String flowId) {
        return true;
    }

    @Override
    public void handle(FlowContext ctx, FlowIO io, FlowSignal sig) {
        String fid = ctx.flowId;
        // Ищем первый специализированный (кроме самого роутера)
        FlowSignalHandler specialized = handlers.stream()
                .filter(h -> h != this && !(h instanceof DefaultFlowSignalHandler))
                .filter(h -> h.supports(fid))
                .findFirst()
                .orElseGet(() -> handlers.stream()
                        .filter(h -> h instanceof DefaultFlowSignalHandler)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("DefaultFlowSignalHandler bean is missing"))
                );

        specialized.handle(ctx, io, sig);
    }
}