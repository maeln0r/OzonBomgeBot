package ru.maelnor.ozonbomgebot.bot.flow;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleFlowRegistryTest {

    static class DummyFlow implements Flow {
        @Override
        public Optional<FlowSignal> start(FlowContext ctx, FlowIO io) {
            return Optional.of(FlowSignal.cont());
        }

        @Override
        public Optional<FlowSignal> handleUpdate(FlowContext ctx, FlowIO io, Update update) {
            return Optional.empty();
        }
    }

    @Test
    void registerAndCreate_returnsFlowInsideOptional() {
        SimpleFlowRegistry registry = new SimpleFlowRegistry();
        registry.register("dummy", DummyFlow::new);

        Optional<Flow> flowOpt = registry.create("dummy");

        assertThat(flowOpt).isPresent();
        assertThat(flowOpt.get()).isInstanceOf(DummyFlow.class);
    }

    @Test
    void unknownId_returnsEmptyOptional() {
        SimpleFlowRegistry registry = new SimpleFlowRegistry();

        Optional<Flow> flowOpt = registry.create("unknown");

        assertThat(flowOpt).isEmpty();
    }
}