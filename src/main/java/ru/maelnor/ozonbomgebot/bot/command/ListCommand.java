package ru.maelnor.ozonbomgebot.bot.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maelnor.ozonbomgebot.bot.entity.TrackedItem;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class ListCommand extends AbstractBotCommand {

    private static final int TELEGRAM_LIMIT = 4096;
    private static final int SAFE_BUDGET = 200; // запас под заголовок/служебные строки
    private final TrackedItemRepository repo;

    public ListCommand(FlowIO io, TrackedItemRepository repo) {
        super(io, "/list", "Показать список отслеживаемых товаров", false);
        this.repo = repo;
    }

    @Override
    public void execute(Update update) {
        if (update.getMessage() == null) return;
        final Long chatId = update.getMessage().getChatId();

        List<TrackedItem> items = new ArrayList<>(repo.findByChat(chatId));
        if (items.isEmpty()) {
            io.toast(chatId, "Список пуст. Добавь товар командой: /add_ozon");
            return;
        }

        // Сортировка: сначала по времени добавления (если есть), затем по названию
        items.sort(Comparator
                .comparing(TrackedItem::getCreatedAtMs, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TrackedItem::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)));

        NumberFormat money = NumberFormat.getInstance(new Locale("ru", "RU"));

        List<String> lines = new ArrayList<>(items.size() + 2);
        lines.add("Отслеживаемые товары (" + items.size() + "):");
        lines.add("");

        int idx = 1;
        for (TrackedItem it : items) {
            String title = nvl(it.getTitle(), "-");

            Long last = it.getLastPrice();
            Long start = it.getStartPrice();

            String lastStr = (last != null && last > 0) ? money.format(last) + " ₽" : "-";
            String startStr = (start != null && start > 0) ? money.format(start) + " ₽" : "-";
            String pctStr = buildPercent(start, last);

            // Текст порога + вычисление срабатывания 🔥
            String thresholdText = renderThreshold(it, money);
            boolean hot = thresholdTriggered(it, start, last);

            String prefix = hot ? "🔥" : "";

            // 1-я строка: номер + (огонь при срабатывании порога) + название
            lines.add(String.format("%d) %s%s", idx++, prefix, title));

            // 2-я строка: цены (текущая и стартовая) + проценты
            lines.add(String.format("💰 %s (старт: %s) • %s", lastStr, startStr, pctStr));

            // 3-я строка: SKU, статус, порог (если задан)
            String statusText = switch (it.getAvailability()) {
                case AVAILABLE -> "В наличии";
                case OUT_OF_STOCK -> "Нет в наличии";
                case UNKNOWN -> "Статус неизвестен";
                case LONG_OUT_OF_STOCK -> "Давно нет в наличии";
            };

            if (!thresholdText.equals("-")) {
                lines.add(String.format("🏷️ SKU %d • 📦 %s • 🎯 Порог: %s", it.getSku(), statusText, thresholdText));
            } else {
                lines.add(String.format("🏷️ SKU %d • 📦 %s", it.getSku(), statusText));
            }

            lines.add("");
        }

        sendChunked(chatId, String.join("\n", lines));
    }

    private static String nvl(String s, String alt) {
        return (s == null || s.isBlank()) ? alt : s;
    }

    private static String buildPercent(Long start, Long last) {
        if (start == null || start <= 0 || last == null || last < 0) return "-";
        long diff = last - start;
        double pct = (diff * 100.0) / start;
        if (Math.abs(pct) < 0.05) return "0%";
        String fmt = (Math.abs(pct) < 10 ? "%.1f" : "%.0f");
        return (pct > 0 ? "+" : "") + String.format(fmt, pct) + "%";
    }

    /**
     * Текст порога для отображения.
     */
    private static String renderThreshold(TrackedItem it, NumberFormat money) {
        ThresholdType type = it.getThresholdType();
        if (type == null) return "-";

        switch (type) {
            case PERCENT -> {
                Long p = it.getThresholdValue();
                return (p != null && p > 0) ? (p + "%") : "-";
            }
            case PRICE -> {
                Long price = it.getThresholdValue();
                return (price != null && price > 0) ? money.format(price) + " ₽" : "-";
            }
            default -> {
                return "-";
            }
        }
    }

    /**
     * Проверка, что индивидуальный порог товара сработал.
     */
    private static boolean thresholdTriggered(TrackedItem it, Long start, Long last) {
        ThresholdType type = it.getThresholdType();
        if (type == null) return false;
        if (last == null || last <= 0) return false;

        switch (type) {
            case PERCENT -> {
                Long p = it.getThresholdValue();
                if (p == null || p <= 0 || start == null || start <= 0) return false;
                double dropPct = (start - last) * 100.0 / start;
                return dropPct >= p - 1e-9;
            }
            case PRICE -> {
                Long price = it.getThresholdValue();
                if (price == null || price <= 0) return false;
                return last <= price;
            }
            default -> {
                return false;
            }
        }
    }

    private void sendChunked(Long chatId, String fullText) {
        if (fullText.length() <= TELEGRAM_LIMIT) {
            io.toast(chatId, fullText);
            return;
        }
        String[] lines = fullText.split("\n");
        String header = lines.length > 0 ? lines[0] : "Отслеживаемые товары:";
        StringBuilder chunk = new StringBuilder(TELEGRAM_LIMIT - SAFE_BUDGET);
        chunk.append(header).append("\n\n");
        for (int i = 1; i < lines.length; i++) {
            String ln = lines[i] + "\n";
            if (chunk.length() + ln.length() > TELEGRAM_LIMIT) {
                io.toast(chatId, chunk.toString().trim());
                chunk.setLength(0);
                chunk.append(header).append(" (продолжение)\n\n");
            }
            chunk.append(ln);
        }
        if (!chunk.isEmpty()) {
            io.toast(chatId, chunk.toString().trim());
        }
    }
}
