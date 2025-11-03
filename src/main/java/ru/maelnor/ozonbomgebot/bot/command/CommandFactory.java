package ru.maelnor.ozonbomgebot.bot.command;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CommandFactory {
    private final List<BotCommand> commands;

    // Все команды автоматически добавятся через DI
    public CommandFactory(List<BotCommand> commands) {
        this.commands = commands;
    }

    public Optional<BotCommand> getCommand(String input) {
        return commands.stream()
                .filter(command -> command.supports(input))
                .findFirst();
    }
}

