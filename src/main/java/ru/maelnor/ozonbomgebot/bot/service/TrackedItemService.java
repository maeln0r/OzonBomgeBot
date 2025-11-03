package ru.maelnor.ozonbomgebot.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maelnor.ozonbomgebot.bot.entity.TrackedItem;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrackedItemService {
    private final TrackedItemRepository repo;
    private final PriceHistoryService history;

    /**
     * Добавление/обновление карточки + запись в глобальную историю цен по SKU.
     * В tracked_item.last_price храним кэш последней цены для быстрого показа списков.
     */
    @Transactional
    public void upsertAndRecord(Long userId,
                                Long chatId,
                                Long sku,
                                String title,
                                Long currentPrice,
                                ProductAvailability availability) {
        long now = System.currentTimeMillis();

        var existingOpt = repo.findByChatIdAndSku(chatId, sku);
        if (existingOpt.isEmpty()) {
            var item = TrackedItem.builder()
                    .chatId(chatId)
                    .userId(userId)
                    .sku(sku)
                    .title(title)
                    .startPrice(currentPrice)
                    .lastPrice(currentPrice)
                    .availability(availability)
                    .createdAtMs(now)
                    .updatedAtMs(now)
                    .build();
            repo.save(item);
            if (currentPrice != null) history.append(sku, currentPrice, availability);
            return;
        }

        // update существующей карточки
        TrackedItem item = existingOpt.get();
        if (title != null && !title.isBlank()) item.setTitle(title);
        item.setAvailability(availability);
        item.setUpdatedAtMs(now);

        if (item.getStartPrice() == null && currentPrice != null) {
            item.setStartPrice(currentPrice);
        }

        if (currentPrice != null) {
            Long lastGlobal = history.getLastPrice(sku).orElse(item.getLastPrice());
            if (!currentPrice.equals(lastGlobal)) {
                history.append(sku, currentPrice, availability);
            }
            item.setLastPrice(currentPrice); // кэш под список
        }

        repo.save(item);
    }

    /**
     * Установить/обновить порог для карточки (chatId, sku).
     * Если карточки нет - создаем пустую с порогом.
     *
     */
    @Transactional
    public void setThreshold(Long chatId, Long userId, Long sku, ThresholdType type, Long value) {
        long now = System.currentTimeMillis();
        var opt = repo.findByChatIdAndSku(chatId, sku);
        if (opt.isEmpty()) {
            var item = TrackedItem.builder()
                    .chatId(chatId)
                    .userId(userId)
                    .sku(sku)
                    .thresholdType(type)
                    .thresholdValue(value)
                    .availability(ProductAvailability.UNKNOWN)
                    .createdAtMs(now)
                    .updatedAtMs(now)
                    .build();
            repo.save(item);
            return;
        }

        var item = opt.get();
        item.setThresholdType(type);
        item.setThresholdValue(value);
        item.setUpdatedAtMs(now);
        repo.save(item);
    }

    @Transactional
    public Optional<TrackedItem> removeTracked(Long chatId, Long sku) {
        return repo.deleteByChatIdAndSku(chatId, sku);
    }

    @Transactional(readOnly = true)
    public Optional<TrackedItem> findTracked(long chatId, long sku) {
        return repo.findByChatIdAndSku(chatId, sku);
    }
}