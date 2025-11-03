package ru.maelnor.ozonbomgebot.bot.flow.pricehistory.state;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.Next;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PriceHistoryAskSkuStateTest {


    @Test
    void enter_sendsPrompt_andStoresMessageId() {
        FlowIO io = mock(FlowIO.class);
        when(io.send(anyLong(), anyString(), any())).thenReturn(222);

        FlowContext ctx = new FlowContext(111L, 222L, UUID.randomUUID(), Instant.now().plusSeconds(60));

        var state = new AskSkuState();
        Next next = state.enter(ctx, io);

        assertThat(next).isInstanceOf(Next.Stay.class);
        assertThat(ctx.lastMessageId).isEqualTo(222);
        verify(io).send(eq(111L), contains("Введите артикул товара"), any());
    }

    @Test
    void onText_validSku_putsToContext_andDone() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(111L, 222L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.lastMessageId = 222;

        var state = new AskSkuState();
        Next next = state.onText(ctx, io, "291003311");

        assertThat(next).isInstanceOf(Next.Done.class);
        assertThat(ctx.<Long>get("sku")).isEqualTo(291003311L);
        verifyNoMoreInteractions(io);
    }

    @Test
    void onText_invalidSku_editsMessage_andStay() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(111L, 222L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.lastMessageId = 333;

        var state = new AskSkuState();
        Next next = state.onText(ctx, io, "abc");

        assertThat(next).isInstanceOf(Next.Stay.class);
        verify(io).edit(eq(111L), eq(333), contains("Некорректный артикул"), isNull());
    }

    @Test
    void onCallback_cancel_returnsCancel() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(111L, 222L, UUID.randomUUID(), Instant.now().plusSeconds(60));

        var state = new AskSkuState();
        Next next = state.onCallback(ctx, io, "price_history:cancel");

        assertThat(next).isInstanceOf(Next.Cancel.class);
    }
}
