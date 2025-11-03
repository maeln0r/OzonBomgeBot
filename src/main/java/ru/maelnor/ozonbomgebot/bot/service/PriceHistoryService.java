package ru.maelnor.ozonbomgebot.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maelnor.ozonbomgebot.bot.entity.PriceHistory;
import ru.maelnor.ozonbomgebot.bot.model.PricePoint;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.repository.PriceHistoryRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {
    private final PriceHistoryRepository repo;

    @Transactional(readOnly = true)
    public Optional<Long> getLastPrice(Long sku) {
        return repo.findLastBySku(sku).map(PriceHistory::getPrice);
    }

    @Transactional
    public void append(Long sku, Long price, ProductAvailability availability) {
        repo.save(PriceHistory.builder()
                .sku(sku)
                .price(price)
                .availability(availability)
                .createdAtMs(System.currentTimeMillis())
                .build());
    }

    /**
     * Последний факт сканирования по SKU.
     */
    public Optional<Instant> findLastScanTime(long sku) {
        return repo.findTopBySkuOrderByCreatedAtMsDesc(sku)
                .map(ph -> Instant.ofEpochMilli(ph.getCreatedAtMs()));
    }

    /**
     * Последний раз, когда товар был в наличии.
     */
    public Optional<Instant> findLastAvailableTime(long sku) {
        return repo.findTopBySkuAndAvailabilityOrderByCreatedAtMsDesc(sku, ProductAvailability.AVAILABLE)
                .map(ph -> Instant.ofEpochMilli(ph.getCreatedAtMs()));
    }

    /**
     * Получаем список цен для графика
     */
    @Transactional(readOnly = true)
    public List<PricePoint> getHistory(long sku) {
        return repo.findAllBySkuOrderByCreatedAtMsAsc(sku).stream()
                .map(ph -> new PricePoint(ph.getPrice(), ph.getCreatedAtMs()))
                .toList();
    }
}