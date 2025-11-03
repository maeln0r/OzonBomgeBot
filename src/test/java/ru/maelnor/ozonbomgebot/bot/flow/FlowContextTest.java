package ru.maelnor.ozonbomgebot.bot.flow;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlowContextTest {

    @Test
    void ctor_setsFinalFields_andBagIsEmpty() {
        long chatId = 123L;
        long userId = 456L;
        UUID sessionId = UUID.randomUUID();
        Instant expires = Instant.now().plusSeconds(60);

        FlowContext ctx = new FlowContext(chatId, userId, sessionId, expires);

        assertThat(ctx.chatId).isEqualTo(chatId);
        assertThat(ctx.userId).isEqualTo(userId);
        assertThat(ctx.sessionId).isEqualTo(sessionId);
        assertThat(ctx.expiresAt).isEqualTo(expires);
        // динамические поля по умолчанию
        assertThat(ctx.flowId).isNull();
        assertThat(ctx.currentStateId).isNull();
        assertThat(ctx.lastMessageId).isNull();
    }

    @Test
    void bag_putAndGet_roundtrip() {
        FlowContext ctx = new FlowContext(1L, 2L, UUID.randomUUID(), Instant.now().plusSeconds(60));

        ctx.put("sku", 291003311L);
        ctx.put("step", "askSku");

        Long sku = ctx.get("sku");
        String step = ctx.get("step");

        assertThat(sku).isEqualTo(291003311L);
        assertThat(step).isEqualTo("askSku");
    }
}