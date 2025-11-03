package ru.maelnor.ozonbomgebot.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Основной интерфейс для команд бота
 */
public interface BotCommand {
    /**
     * Проверка, обрабатывает ли команда этот ввод
     *
     * @param command
     * @return
     */
    boolean supports(String command);

    /**
     * Проверка на необходимость админских прав
     *
     * @return
     */
    public boolean isAdminOnly();

    /**
     * Основная логика обработки команды
     *
     * @param update
     */
    void execute(Update update);

    /**
     * Название команды
     *
     * @return Название команды
     */
    String getCommandName();

    /**
     * Описание команды
     *
     * @return Описание команды
     */
    String getCommandDescription();
}
