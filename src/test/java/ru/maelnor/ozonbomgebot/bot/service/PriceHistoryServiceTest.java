package ru.maelnor.ozonbomgebot.bot.service;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.entity.PriceHistory;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.repository.PriceHistoryRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PriceHistoryServiceTest {

    private final PriceHistoryRepository repo = mock(PriceHistoryRepository.class);
    private final PriceHistoryService service = new PriceHistoryService(repo);

    @Test
    void getLastPrice_delegatesToRepo() {
        when(repo.findLastBySku(291003311L))
                .thenReturn(Optional.of(PriceHistory.builder()
                        .sku(291003311L)
                        .price(4105L)
                        .availability(ProductAvailability.AVAILABLE)
                        .createdAtMs(1L)
                        .build()));

        var opt = service.getLastPrice(291003311L);

        assertThat(opt).contains(4105L);
    }

    @Test
    void append_savesEntityWithNow() {
        service.append(291003311L, 4105L, ProductAvailability.AVAILABLE);

        verify(repo).save(argThat(ph ->
                ph.getSku().equals(291003311L)
                        && ph.getPrice().equals(4105L)
                        && ph.getAvailability() == ProductAvailability.AVAILABLE
                        && ph.getCreatedAtMs() != null
        ));
    }

    @Test
    void findLastScanTime_returnsInstantFromRepo() {
        long now = System.currentTimeMillis();
        when(repo.findTopBySkuOrderByCreatedAtMsDesc(1L))
                .thenReturn(Optional.of(PriceHistory.builder()
                        .sku(1L)
                        .price(100L)
                        .availability(ProductAvailability.UNKNOWN)
                        .createdAtMs(now)
                        .build()));

        var opt = service.findLastScanTime(1L);

        assertThat(opt).contains(Instant.ofEpochMilli(now));
    }

    @Test
    void findLastAvailableTime_returnsInstantFromRepo() {
        long now = System.currentTimeMillis();
        when(repo.findTopBySkuAndAvailabilityOrderByCreatedAtMsDesc(1L, ProductAvailability.AVAILABLE))
                .thenReturn(Optional.of(PriceHistory.builder()
                        .sku(1L)
                        .price(100L)
                        .availability(ProductAvailability.AVAILABLE)
                        .createdAtMs(now)
                        .build()));

        var opt = service.findLastAvailableTime(1L);

        assertThat(opt).contains(Instant.ofEpochMilli(now));
    }
}
