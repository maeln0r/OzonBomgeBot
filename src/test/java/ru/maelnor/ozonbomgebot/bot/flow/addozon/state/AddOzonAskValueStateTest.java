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

class AddOzonAskValueStateTest {

    @Test
    void onText_percent_valid_storesPercent_andDone() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.put("type", ThresholdType.PERCENT);

        var state = new AskValueState();
        Next next = state.onText(ctx, io, "15");

        assertThat(next).isInstanceOf(Next.Done.class);
        assertThat(ctx.<Long>get("percent")).isEqualTo(15L);
        verifyNoMoreInteractions(io);
    }

    @Test
    void onText_price_valid_storesPrice_andDone() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.put("type", ThresholdType.PRICE);

        var state = new AskValueState();
        Next next = state.onText(ctx, io, "4990");

        assertThat(next).isInstanceOf(Next.Done.class);
        assertThat(ctx.<Long>get("price")).isEqualTo(4990L);
        verifyNoMoreInteractions(io);
    }

    @Test
    void onText_invalid_showsToast_andStay() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.put("type", ThresholdType.PERCENT);

        var state = new AskValueState();
        Next next = state.onText(ctx, io, "abc");

        assertThat(next).isInstanceOf(Next.Stay.class);
        verify(io).toast(eq(123L), contains("Некорректный процент"));
    }

    @Test
    void onCallback_back_returnsGotoAskType_andEditsMessage() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.lastMessageId = 777;
        ctx.put("sku", 291003311L);

        var state = new AskValueState();
        Next next = state.onCallback(ctx, io, "add_ozon:back");

        assertThat(next).isInstanceOf(Next.Goto.class);
        assertThat(((Next.Goto) next).stateId()).isEqualTo("askType");
        verify(io).edit(eq(123L), eq(777), contains("Выберите порог срабатывания"), any());
    }

    @Test
    void onCallback_cancel_returnsCancel() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));

        var state = new AskValueState();
        Next next = state.onCallback(ctx, io, "add_ozon:cancel");

        assertThat(next).isInstanceOf(Next.Cancel.class);
    }
}
