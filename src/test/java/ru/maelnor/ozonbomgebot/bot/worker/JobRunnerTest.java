package ru.maelnor.ozonbomgebot.bot.worker;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.entity.Job;
import ru.maelnor.ozonbomgebot.bot.entity.PriceHistory;
import ru.maelnor.ozonbomgebot.bot.entity.TrackedItem;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.model.ProductInfo;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;
import ru.maelnor.ozonbomgebot.bot.model.job.JobType;
import ru.maelnor.ozonbomgebot.bot.repository.PriceHistoryRepository;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;
import ru.maelnor.ozonbomgebot.bot.service.JobQueueService;
import ru.maelnor.ozonbomgebot.bot.service.integration.ozon.OzonPriceService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobRunnerTest {

    private final JobQueueService jobQueueService = mock(JobQueueService.class);
    private final OzonPriceService ozonPriceService = mock(OzonPriceService.class);
    private final TrackedItemRepository trackedRepo = mock(TrackedItemRepository.class);
    private final PriceHistoryRepository priceRepo = mock(PriceHistoryRepository.class);
    private final FlowIO io = mock(FlowIO.class);

    private final JobRunner runner = new JobRunner(
            jobQueueService,
            ozonPriceService,
            trackedRepo,
            priceRepo,
            io
    );

    private Job makeScanJob(long sku) {
        return Job.builder()
                .id(1)
                .type(JobType.SCAN_SKU)
                .payloadJson("{\"sku\":" + sku + "}")
                .build();
    }

    @Test
    void run_scanSku_updatesTrackedAndWritesHistory() throws Exception {
        long sku = 291003311L;
        Job job = makeScanJob(sku);

        when(jobQueueService.readPayload(job)).thenReturn(Map.of("sku", sku));
        when(ozonPriceService.fetch(sku))
                .thenReturn(new ProductInfo("Тестовый товар", sku, 4105L, ProductAvailability.AVAILABLE));

        TrackedItem tracked = TrackedItem.builder()
                .id(1)
                .chatId(123L)
                .sku(sku)
                .availability(ProductAvailability.UNKNOWN)
                .build();
        when(trackedRepo.findTrackedItemsBySku(sku)).thenReturn(List.of(tracked));

        runner.run(job);

        // обновили подписчика
        verify(trackedRepo, atLeastOnce()).save(argThat(t ->
                t.getSku().equals(sku)
                        && t.getTitle().equals("Тестовый товар")
                        && t.getLastPrice().equals(4105L)
                        && t.getAvailability() == ProductAvailability.AVAILABLE
        ));

        // записали историю
        verify(priceRepo).save(argThat(ph ->
                ph.getSku().equals(sku)
                        && ph.getPrice().equals(4105L)
                        && ph.getAvailability() == ProductAvailability.AVAILABLE
        ));

        // уведомление НЕ слали — порога нет
        verifyNoInteractions(io);
    }

    @Test
    void run_scanSku_thresholdMatched_sendsToast() throws Exception {
        long sku = 291003311L;
        Job job = makeScanJob(sku);

        when(jobQueueService.readPayload(job)).thenReturn(Map.of("sku", sku));
        // цена упала с 5000 до 4000 → 20%
        when(ozonPriceService.fetch(sku))
                .thenReturn(new ProductInfo("Тест", sku, 4000L, ProductAvailability.AVAILABLE));

        TrackedItem tracked = TrackedItem.builder()
                .id(1)
                .chatId(999L)
                .sku(sku)
                .title("Тест")
                .startPrice(5000L)
                .lastPrice(5000L)
                .availability(ProductAvailability.AVAILABLE)
                .thresholdType(ThresholdType.PERCENT)
                .thresholdValue(15L) // сработает
                .build();
        when(trackedRepo.findTrackedItemsBySku(sku)).thenReturn(List.of(tracked));

        runner.run(job);

        verify(io).toast(eq(999L), contains("Цена изменилась по SKU"));
    }

    @Test
    void run_scanSku_longOutOfStock_enqueuesNotifyJob() throws Exception {
        long sku = 291003311L;
        Job job = makeScanJob(sku);

        when(jobQueueService.readPayload(job)).thenReturn(Map.of("sku", sku));
        // сейчас товар OUT_OF_STOCK
        when(ozonPriceService.fetch(sku))
                .thenReturn(new ProductInfo("Тест", sku, 0L, ProductAvailability.OUT_OF_STOCK));

        // подписчики есть
        TrackedItem tracked = TrackedItem.builder()
                .id(1)
                .chatId(999L)
                .sku(sku)
                .availability(ProductAvailability.OUT_OF_STOCK)
                .build();
        when(trackedRepo.findTrackedItemsBySku(sku)).thenReturn(List.of(tracked));

        // последняя AVAILABLE была 40 дней назад → больше 30 → надо слать уведомление
        long now = Instant.now().toEpochMilli();
        long fortyDaysAgo = now - Duration.ofDays(40).toMillis();
        when(priceRepo.findTopBySkuAndAvailabilityOrderByCreatedAtMsDesc(sku, ProductAvailability.AVAILABLE))
                .thenReturn(Optional.of(
                        PriceHistory.builder()
                                .sku(sku)
                                .price(9999L)
                                .availability(ProductAvailability.AVAILABLE)
                                .createdAtMs(fortyDaysAgo)
                                .build()
                ));

        runner.run(job);

        verify(jobQueueService).enqueueNotifySkuDisabled(sku);
    }
}
