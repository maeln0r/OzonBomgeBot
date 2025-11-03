package ru.maelnor.ozonbomgebot.bot.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;
import ru.maelnor.ozonbomgebot.bot.service.JobQueueService;
import ru.maelnor.ozonbomgebot.bot.service.PriceHistoryService;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Периодически пробегается по отслеживаемым товарам и ставит их в очередь на скан.
 * Делает это с джиттером, чтобы не бахать в Озон пачкой.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceWatcher {

    private final TrackedItemRepository trackedItemRepository;
    private final JobQueueService jobQueueService;
    private final PriceHistoryService priceHistoryService;

    private static final long MIN_SCAN_INTERVAL_MS = 60L * 60L * 1000L;
    private static final long BASE_STEP_MS = 800L;
    private static final long RANDOM_JITTER_MS = 10_000L;

    // ходим каждые 10 минут
    @Scheduled(cron = "${app.scheduler.cron}")
    public void scheduleScans() {
        // 1. Берем уже отсортированный список SKU (по популярности), кроме “уснувших”
        List<Long> skus = trackedItemRepository
                .findSkuOrderByPopularityExcludingAvailability(ProductAvailability.LONG_OUT_OF_STOCK);
        if (skus.isEmpty()) {
            return;
        }

        // 2. Берем последнюю точку в очереди именно для SCAN_SKU
        long baseRunAt = jobQueueService.findMaxRunAtForScanSku()
                .orElse(Instant.now().toEpochMilli());

        long index = 0L;
        for (Long sku : skus) {
            long now = System.currentTimeMillis();

            // 3. проверяем, не сканили ли раньше часа назад
            var lastScanOpt = priceHistoryService.findLastScanTime(sku);
            if (lastScanOpt.isPresent()) {
                long diff = now - lastScanOpt.get().toEpochMilli();
                if (diff < MIN_SCAN_INTERVAL_MS) {
                    continue;
                }
            }

            long jitter = ThreadLocalRandom.current().nextLong(RANDOM_JITTER_MS);
            long runAt = baseRunAt + index * BASE_STEP_MS + jitter;
            jobQueueService.enqueueScanSku(sku, runAt);
            index++;
        }
    }
}