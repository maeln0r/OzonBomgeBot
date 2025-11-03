package ru.maelnor.ozonbomgebot.bot.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.maelnor.ozonbomgebot.bot.entity.Job;
import ru.maelnor.ozonbomgebot.bot.entity.PriceHistory;
import ru.maelnor.ozonbomgebot.bot.entity.TrackedItem;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.model.ProductInfo;
import ru.maelnor.ozonbomgebot.bot.model.job.JobType;
import ru.maelnor.ozonbomgebot.bot.repository.PriceHistoryRepository;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;
import ru.maelnor.ozonbomgebot.bot.service.JobQueueService;
import ru.maelnor.ozonbomgebot.bot.service.integration.ozon.OzonPriceService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JobRunner {
    private final JobQueueService jobQueueService;
    private final OzonPriceService ozonPriceService;
    private final TrackedItemRepository trackedRepo;
    private final PriceHistoryRepository priceRepo;
    private final FlowIO io;
    private final ObjectMapper om = new ObjectMapper();

    // 30 дней на OUT_OF_STOCK
    private static final long LONG_OUT_OF_STOCK_MS = Duration.ofDays(30).toMillis();

    @Transactional
    public void run(Job job) throws Exception {
        if (job.getType() == JobType.SCAN_SKU) {
            handleScanSku(job);
        } else if (job.getType() == JobType.NOTIFY_SKU_DISABLED) {
            handleNotifySkuDisabled(job);
        } else {
            throw new IllegalArgumentException("Unsupported job type: " + job.getType());
        }
    }

    //@Transactional
    protected void handleScanSku(Job job) throws Exception {
        Map<String, Object> payload = jobQueueService.readPayload(job);
        long sku = ((Number) payload.get("sku")).longValue();

        // 1) получаем актуальную инфу
        ProductInfo info = ozonPriceService.fetch(sku);

        long now = Instant.now().toEpochMilli();
        Long price = info.price();
        ProductAvailability avail = info.availability() != null ? info.availability() : ProductAvailability.UNKNOWN;

        // 2) обновляем всех подписчиков (tracked_item) по этому SKU
        List<TrackedItem> subs = trackedRepo.findTrackedItemsBySku(sku);

        for (TrackedItem t : subs) {
            boolean firstFill = (t.getStartPrice() == null || t.getStartPrice() <= 0);
            Long oldLast = t.getLastPrice();
            t.setTitle(info.title());
            if (firstFill) {
                t.setStartPrice(price);
            }
            t.setLastPrice(price);
            t.setAvailability(avail);
            t.setUpdatedAtMs(now);
            trackedRepo.save(t);

            // 3) пишем историю, если цена есть
            if (price != null && price > 0) {
                priceRepo.save(PriceHistory.builder()
                        .sku(sku)
                        .price(price)
                        .availability(avail)
                        .createdAtMs(now)
                        .build());
            }

            // 4) проверка порога и уведомление
            if (shouldNotify(t)) {
                notifyDrop(t, oldLast, price);
            }
        }

        // если товар уже 30 дней не был в наличии - создаем отдельную задачу-уведомление
        if (avail == ProductAvailability.OUT_OF_STOCK) {
            // достаем последнюю запись в истории, когда он вообще был AVAILABLE
            priceRepo.findTopBySkuAndAvailabilityOrderByCreatedAtMsDesc(sku, ProductAvailability.AVAILABLE)
                    .ifPresentOrElse(ph -> {
                        long diff = now - ph.getCreatedAtMs();
                        if (diff > LONG_OUT_OF_STOCK_MS) {
                            jobQueueService.enqueueNotifySkuDisabled(sku);
                            subs.forEach(t -> t.setAvailability(ProductAvailability.LONG_OUT_OF_STOCK));
                            trackedRepo.saveAll(subs);
                        }
                    }, () -> {
                        // если НИ РАЗУ не был в наличии и мы его уже месяц сканим
                    });
        }
    }

    // Обработка NOTIFY_SKU_DISABLED (простое уведомление всем, кто подписан)
    private void handleNotifySkuDisabled(Job job) throws Exception {
        Map<String, Object> payload = jobQueueService.readPayload(job);
        long sku = ((Number) payload.get("sku")).longValue();
        List<TrackedItem> subs = trackedRepo.findTrackedItemsBySku(sku);
        for (TrackedItem t : subs) {
            io.toast(t.getChatId(), "📦 Товар по SKU " + sku + " долго недоступен на OZON. Автосканирование приостановлено.");
        }
    }

    private boolean shouldNotify(TrackedItem t) {
        if (t.getThresholdType() == null || t.getThresholdValue() == null) return false;
        Long start = t.getStartPrice();
        Long last = t.getLastPrice();
        if (last == null || last <= 0) return false;

        return switch (t.getThresholdType()) {
            case PERCENT -> {
                if (start == null || start <= 0) yield false;
                double drop = (start - last) * 100.0 / start;
                yield drop >= t.getThresholdValue();
            }
            case PRICE -> last <= t.getThresholdValue();
        };
    }

    private void notifyDrop(TrackedItem t, Long was, Long now) {
        Long chatId = t.getChatId();
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 Цена изменилась по SKU ").append(t.getSku()).append("\n");
        if (t.getTitle() != null && !t.getTitle().isBlank()) {
            sb.append(t.getTitle()).append("\n");
        }
        if (was != null) sb.append("Было: ").append(was).append(" ₽\n");
        if (now != null) sb.append("Стало: ").append(now).append(" ₽\n");

        switch (t.getThresholdType()) {
            case PERCENT -> sb.append("Порог: ≥ ").append(t.getThresholdValue()).append("% падения");
            case PRICE -> sb.append("Порог: ≤ ").append(t.getThresholdValue()).append(" ₽");
        }

        io.toast(chatId, sb.toString());
    }
}