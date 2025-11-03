package ru.maelnor.ozonbomgebot.bot.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class SimpleFlowRegistry implements FlowRegistry {
    private final Map<String, Supplier<Flow>> factories = new HashMap<>();

    public SimpleFlowRegistry register(String flowId, java.util.function.Supplier<Flow> factory) {
        factories.put(flowId, factory);
        return this;
    }

    @Override
    public Optional<Flow> create(String flowId) {
        return Optional.ofNullable(factories.get(flowId)).map(java.util.function.Supplier::get);
    }
}
