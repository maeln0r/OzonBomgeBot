package ru.maelnor.ozonbomgebot.bot.flow;


import java.io.File;

/**
 * Сигналы от flow к внешнему миру.
 * I/O-сигналы (Send/Edit/Toast/AnswerCallback) обычно обрабатывает FlowSignalHandler (через FlowIO),
 * а жизненный цикл (Done/Cancel) - SessionManager/Bridge.
 */
public sealed interface FlowSignal
        permits FlowSignal.Send,
        FlowSignal.Edit,
        FlowSignal.Toast,
        FlowSignal.Continue,
        FlowSignal.Done,
        FlowSignal.Cancel,
        FlowSignal.SendPhoto{

    /**
     * Отправить новое сообщение.
     */
    record Send(long chatId, String text, FlowIO.Keyboard keyboard) implements FlowSignal {
    }

    /**
     * Отредактировать существующее сообщение.
     */
    record Edit(long chatId, Integer messageId, String text, FlowIO.Keyboard keyboard) implements FlowSignal {
    }

    /**
     * Короткое одноразовое сообщение (уведомление/чанк/т.п.).
     */
    record Toast(long chatId, String text) implements FlowSignal {
    }

    /**
     * Продолжить без побочных эффектов
     */
    record Continue() implements FlowSignal {
    }

    /**
     * Завершить сессию успешно.
     */
    record Done(long chatId, Integer messageId, String text) implements FlowSignal {
    }

    /**
     * Завершить сессию с причиной отмены.
     */
    record Cancel(long chatId, Integer messageId, String reason) implements FlowSignal {
    }

    /**
     * Отправить фото (документ), возможно с подписью.
     */
    record SendPhoto(long chatId, File file, String caption) implements FlowSignal {
    }

    /* ---------- Утилиты для лаконичного создания сигналов ---------- */

    static Send send(long chatId, String text) {
        return new Send(chatId, text, null);
    }

    static Send send(long chatId, String text, FlowIO.Keyboard keyboard) {
        return new Send(chatId, text, keyboard);
    }

    static Edit edit(long chatId, int messageId, String text) {
        return new Edit(chatId, messageId, text, null);
    }

    static Edit edit(long chatId, int messageId, String text, FlowIO.Keyboard keyboard) {
        return new Edit(chatId, messageId, text, keyboard);
    }

    static Toast toast(long chatId, String text) {
        return new Toast(chatId, text);
    }

    static Continue cont() {
        return new Continue();
    }

    static Done done(long chatId, int messageId, String text) {
        return new Done(chatId, messageId, text);
    }

    static Cancel cancel(long chatId, int messageId) {
        return new Cancel(chatId, messageId, null);
    }

    static Cancel cancel(long chatId, int messageId, String reason) {
        return new Cancel(chatId, messageId, reason);
    }

    static SendPhoto photo(long chatId, File file, String caption) {
        return new SendPhoto(chatId, file, caption);
    }
}
