package ru.maelnor.ozonbomgebot.bot.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class BotCommandRegistrar {
    private final TelegramClient telegramClient;
    private final List<ru.maelnor.ozonbomgebot.bot.command.BotCommand> botCommands;

    public BotCommandRegistrar(TelegramClient telegramClient, List<ru.maelnor.ozonbomgebot.bot.command.BotCommand> botCommands) {
        this.telegramClient = telegramClient;
        this.botCommands = botCommands;
    }

    @PostConstruct
    public void registerCommands() {
        List<BotCommand> commands = new ArrayList<>();
        for (ru.maelnor.ozonbomgebot.bot.command.BotCommand command : botCommands) {
            if (command.getCommandName() != null && command.getCommandDescription() != null) {
                commands.add(new BotCommand(command.getCommandName(), command.getCommandDescription()));
            }
        }

        commands.sort((a, b) -> {
            String na = a.getCommand();
            String nb = b.getCommand();
            if ("/help".equalsIgnoreCase(na)) return -1;
            if ("/help".equalsIgnoreCase(nb)) return 1;
            return na.compareToIgnoreCase(nb);
        });


        SetMyCommands setMyCommands = new SetMyCommands(commands);

        try {
            telegramClient.execute(setMyCommands);
            log.info("Команды бота успешно зарегистрированы.");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            log.error("Ошибка при регистрации команд бота: {}", e.getMessage());
        }
    }
}

