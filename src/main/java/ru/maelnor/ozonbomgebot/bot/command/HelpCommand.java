package ru.maelnor.ozonbomgebot.bot.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HelpCommand extends AbstractBotCommand {
    private final List<BotCommand> botCommands;

    public HelpCommand(FlowIO io, List<BotCommand> botCommands) {
        super(io, "/help", "Показать справку по командам бота", false);
        this.botCommands = botCommands;
    }

    @Override
    public void execute(Update update) {
        if (update.getMessage() == null) return;
        final Long chatId = update.getMessage().getChatId();

        String text = buildHelpText();
        // На всякий - режем, если внезапно станет длиннее лимита Telegram (~4096)
        for (String chunk : splitForTelegram(text, 3800)) {
            io.toast(chatId, chunk);
        }
    }

    private String buildHelpText() {
        StringBuilder out = new StringBuilder();

        out.append("Привет! Я отслеживаю цену и доступность товаров Ozon по артикулу (SKU).\n")
                .append("\n")
                .append("Как работает:\n")
                .append("- Ты добавляешь товар по SKU.\n")
                .append("- Бот периодически сам обновляет данные и уведомляет об изменениях.\n")
                .append("\n")
                .append("Где найти артикул (SKU):\n")
                .append("- С компьютера: на странице товара над блоком с ценой (копируется кликом).\n")
                .append("- С телефона: верхняя строка в блоке «Характеристики», копируется по нажатию.\n")
                .append("Как отредактировать товар:\n")
                .append("- Запустить команду /add_ozon с указанием существующего SKU.\n")
                .append("\n");

        List<BotCommand> commands = safeCommands();

        if (!commands.isEmpty()) {
            out.append("Команды:\n");

            for (BotCommand c : commands) {
                out.append(c.getCommandName())
                        .append(" ")
                        .append("- ")
                        .append(c.getCommandDescription())
                        .append("\n");
            }
        }

        out.append("\nПодсказка:\n")
                .append("- SKU - это только цифры без пробелов и символов.\n");

        return out.toString();
    }

    private List<BotCommand> safeCommands() {
        if (botCommands == null) return List.of();

        Map<String, BotCommand> byName = new LinkedHashMap<>();
        for (BotCommand c : botCommands) {
            if (c == null) continue;
            String name = safe(c.getCommandName());
            String desc = safe(c.getCommandDescription());
            if (name.isBlank() || desc.isBlank()) continue;
            byName.putIfAbsent(name, c);
        }

        List<BotCommand> list = new ArrayList<>(byName.values());

        list.sort((a, b) -> {
            String na = safe(a.getCommandName());
            String nb = safe(b.getCommandName());
            if ("/help".equals(na)) return -1;
            if ("/help".equals(nb)) return 1;
            return na.compareToIgnoreCase(nb);
        });
        return list;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static List<String> splitForTelegram(String text, int maxLen) {
        if (text == null) return List.of("");
        if (text.length() <= maxLen) return List.of(text);

        List<String> parts = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(text.length(), i + maxLen);
            int cut = text.lastIndexOf('\n', end);
            if (cut <= i) cut = end;
            parts.add(text.substring(i, cut));
            i = cut;
        }
        return parts.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }
}
