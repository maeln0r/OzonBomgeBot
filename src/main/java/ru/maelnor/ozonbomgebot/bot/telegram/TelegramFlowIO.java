package ru.maelnor.ozonbomgebot.bot.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;

import java.util.ArrayList;
import java.util.List;

public final class TelegramFlowIO implements FlowIO {
    private final TelegramClient client;

    public TelegramFlowIO(TelegramClient client) {
        this.client = client;
    }

    @Override
    public Integer send(long chatId, String text, Keyboard kb) {
        var sm = new SendMessage(Long.toString(chatId), text);
        if (kb != null) sm.setReplyMarkup(toMarkup(kb));
        try {
            var msg = client.execute(sm);
            return msg.getMessageId();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void edit(long chatId, Integer messageId, String text, Keyboard kb) {
        var em = EditMessageText.builder()
                .messageId(messageId)
                .chatId(Long.toString(chatId))
                .text(text)
                .build();
        if (kb != null) em.setReplyMarkup(toMarkup(kb));
        try {
            client.execute(em);
        } catch (Exception ignored) {
            System.out.println(ignored.getMessage());
        }
    }

    @Override
    public void toast(long chatId, String text) {
        send(chatId, text, null);
    }

    @Override
    public void sendPhoto(long chatId, java.io.File file, String caption) {
        var photo = SendPhoto.builder()
                .chatId(Long.toString(chatId))
                .caption(caption)
                .photo(new InputFile(file))
                .build();
        try {
            client.execute(photo);
        } catch (Exception e) {
        }
    }

    private static InlineKeyboardMarkup toMarkup(Keyboard k) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (var r : k.rows()) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            for (var b : r) {
                var btn = new InlineKeyboardButton(b.text());
                btn.setCallbackData(b.data());
                row.add(btn);
            }
            rows.add(row);
        }
        var m = InlineKeyboardMarkup.builder().keyboard(rows).build();
        m.setKeyboard(rows);
        return m;
    }
}