package ru.maelnor.ozonbomgebot.bot.command;

import org.springframework.beans.factory.annotation.Value;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;

public abstract class AbstractBotCommand implements BotCommand {
    protected final FlowIO io;
    private final String commandName;
    private final String commandDescription;
    private final boolean adminOnly;
    @Value("${app.bot.name}")
    private String botName;

    public AbstractBotCommand(FlowIO io, String commandName, String commandDescription, boolean adminOnly) {
        this.io = io;
        this.commandName = commandName;
        this.commandDescription = commandDescription;
        this.adminOnly = adminOnly;
    }

    @Override
    public boolean isAdminOnly() {
        return adminOnly;
    }

    @Override
    public boolean supports(String text) {
        if (text == null) return false;

        String trimmed = text.trim();
        if (!trimmed.startsWith("/")) return false;

        // Берем первый токен до пробела
        String first = trimmed.split("\\s+", 2)[0];

        String base = getCommandName();
        String withMention = base + "@" + botName;

        return first.equals(base) || first.equalsIgnoreCase(withMention);
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String getCommandDescription() {
        return commandDescription;
    }
}

