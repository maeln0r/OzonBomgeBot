package ru.maelnor.ozonbomgebot.bot.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TelegramFlowIOTest {

    @Test
    @DisplayName("send: при успехе возвращает messageId из Telegram")
    void send_whenOk_returnsMessageId() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        Message msg = new Message();
        msg.setMessageId(42);
        when(client.execute(any(SendMessage.class))).thenReturn(msg);

        TelegramFlowIO io = new TelegramFlowIO(client);

        Integer id = io.send(123L, "hello", null);

        assertEquals(42, id);
        verify(client, times(1)).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("send: при исключении возвращает -1")
    void send_whenException_returnsMinusOne() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        when(client.execute(any(SendMessage.class))).thenThrow(new RuntimeException("tg down"));

        TelegramFlowIO io = new TelegramFlowIO(client);

        Integer id = io.send(123L, "hello", null);

        assertEquals(-1, id);
        verify(client, times(1)).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("toast: просто делегирует в send без клавы")
    void toast_delegatesToSend() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        Message msg = new Message();
        msg.setMessageId(1);
        when(client.execute(any(SendMessage.class))).thenReturn(msg);

        TelegramFlowIO io = new TelegramFlowIO(client);

        io.toast(777L, "ping");

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(client).execute(captor.capture());
        SendMessage sm = captor.getValue();
        assertEquals("777", sm.getChatId());
        assertEquals("ping", sm.getText());
        assertNull(sm.getReplyMarkup());
    }

    @Test
    @DisplayName("edit: шлёт EditMessageText и пробрасывает клавиатуру, если она есть")
    void edit_sendsEditMessageText() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        TelegramFlowIO io = new TelegramFlowIO(client);

        FlowIO.Keyboard kb = FlowIO.Keyboard.builder()
                .row(new FlowIO.Button("b1", "cb1"))
                .build();

        io.edit(999L, 10, "updated", kb);

        ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(client).execute(captor.capture());
        EditMessageText em = captor.getValue();

        assertEquals("999", em.getChatId());
        assertEquals(10, em.getMessageId());
        assertEquals("updated", em.getText());
        assertNotNull(em.getReplyMarkup());
    }
}
