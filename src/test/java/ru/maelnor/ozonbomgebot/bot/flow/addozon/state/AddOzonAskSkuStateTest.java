package ru.maelnor.ozonbomgebot.bot.flow.addozon.state;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.flow.FlowContext;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.flow.Next;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AddOzonAskSkuStateTest {

    @Test
    void enter_sendsPrompt_andStoresMessageId() {
        FlowIO io = mock(FlowIO.class);
        when(io.send(anyLong(), anyString(), any())).thenReturn(777);

        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));

        var state = new AskSkuState();
        Next next = state.enter(ctx, io);

        assertThat(next).isInstanceOf(Next.Stay.class);
        assertThat(ctx.lastMessageId).isEqualTo(777);

        verify(io).send(eq(123L), contains("Введите артикул товара"), any());
    }

    @Test
    void onText_validSku_putsToContext_andGoesToAskType() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.lastMessageId = 777;

        var state = new AskSkuState();
        Next next = state.onText(ctx, io, "291003311");

        assertThat(next).isInstanceOf(Next.Goto.class);
        assertThat(((Next.Goto) next).stateId()).isEqualTo("askType");
        assertThat(ctx.<Long>get("sku")).isEqualTo(291003311L);

        verify(io).edit(eq(123L), eq(777), contains("Артикул: 291003311"), any());
    }

    @Test
    void onText_invalidSku_showsError_andStay() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));
        ctx.lastMessageId = 777;

        var state = new AskSkuState();
        Next next = state.onText(ctx, io, "abc");

        assertThat(next).isInstanceOf(Next.Stay.class);
        verify(io).toast(eq(123L), contains("Некорректный артикул"));
    }

    @Test
    void onCallback_cancel_returnsCancel() {
        FlowIO io = mock(FlowIO.class);
        FlowContext ctx = new FlowContext(123L, 456L, UUID.randomUUID(), Instant.now().plusSeconds(60));

        var state = new AskSkuState();
        Next next = state.onCallback(ctx, io, "add_ozon:cancel");

        assertThat(next).isInstanceOf(Next.Cancel.class);
        assertThat(((Next.Cancel) next).reason()).contains("пользовательская");
    }
}
