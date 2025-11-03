package ru.maelnor.ozonbomgebot.bot.flow.addozon.state;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.Next;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AddOzonAskTypeStateTest {

    @Test
    void onCallback_percent_setsType_andAsksPercent_andGotoAskValue() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.lastMessageId = 777;

        var state = new AskTypeState();

        Next next = state.onCallback(ctx, io, "add_ozon:type:PERCENT");

        assertThat(ctx.<ThresholdType>get("type")).isEqualTo(ThresholdType.PERCENT);
        assertThat(next).isInstanceOf(Next.Goto.class);
        assertThat(((Next.Goto) next).stateId()).isEqualTo("askValue");

        verify(io).edit(eq(123L), eq(777), contains("Введите процент"), any());
    }

    @Test
    void onCallback_price_setsType_andAsksPrice_andGotoAskValue() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.lastMessageId = 777;

        var state = new AskTypeState();

        Next next = state.onCallback(ctx, io, "add_ozon:type:PRICE");

        assertThat(ctx.<ThresholdType>get("type")).isEqualTo(ThresholdType.PRICE);
        assertThat(next).isInstanceOf(Next.Goto.class);
        assertThat(((Next.Goto) next).stateId()).isEqualTo("askValue");

        verify(io).edit(eq(123L), eq(777), contains("Введите целевую цену"), any());
    }

    @Test
    void onCallback_cancel_returnsCancel() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));

        var state = new AskTypeState();

        Next next = state.onCallback(ctx, io, "add_ozon:cancel");

        assertThat(next).isInstanceOf(Next.Cancel.class);
    }

    @Test
    void onText_isIgnored() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(1L, 2L, UUID.randomUUID(), Instant.now().plusSeconds(60));

        var state = new AskTypeState();
        Next next = state.onText(ctx, io, "что-то");

        assertThat(next).isInstanceOf(Next.Stay.class);
        verifyNoInteractions(io);
    }
}
