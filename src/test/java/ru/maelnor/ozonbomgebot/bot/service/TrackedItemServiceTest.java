package ru.maelnor.ozonbomgebot.bot.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.maelnor.ozonbomgebot.bot.entity.TrackedItem;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrackedItemServiceTest {

    private final TrackedItemRepository repo = mock(TrackedItemRepository.class);
    private final PriceHistoryService history = mock(PriceHistoryService.class);
    private final TrackedItemService service = new TrackedItemService(repo, history);

    @Test
    void upsertAndRecord_newItem_savesAndWritesHistory() {
        Long userId = 10L, chatId = 20L, sku = 291003311L;
        when(repo.findByChatIdAndSku(chatId, sku)).thenReturn(Optional.empty());

        service.upsertAndRecord(userId, chatId, sku,
                "Товар", 4105L, ProductAvailability.AVAILABLE);

        // сохранили новый
        ArgumentCaptor<TrackedItem> itemCap = ArgumentCaptor.forClass(TrackedItem.class);
        verify(repo).save(itemCap.capture());
        TrackedItem saved = itemCap.getValue();

        assertThat(saved.getChatId()).isEqualTo(chatId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getSku()).isEqualTo(sku);
        assertThat(saved.getTitle()).isEqualTo("Товар");
        assertThat(saved.getStartPrice()).isEqualTo(4105L);
        assertThat(saved.getLastPrice()).isEqualTo(4105L);
        assertThat(saved.getAvailability()).isEqualTo(ProductAvailability.AVAILABLE);
        assertThat(saved.getCreatedAtMs()).isNotNull();
        assertThat(saved.getUpdatedAtMs()).isNotNull();

        // и запись в глобальную историю
        verify(history).append(sku, 4105L, ProductAvailability.AVAILABLE);
    }

    @Test
    void upsertAndRecord_existing_samePrice_updatesButDoesNotWriteHistory() {
        Long chatId = 20L, sku = 291003311L;
        TrackedItem existing = TrackedItem.builder()
                .id(1)
                .chatId(chatId)
                .sku(sku)
                .title("OLD")
                .lastPrice(4105L)
                .availability(ProductAvailability.AVAILABLE)
                .build();
        when(repo.findByChatIdAndSku(chatId, sku)).thenReturn(Optional.of(existing));
        when(history.getLastPrice(sku)).thenReturn(Optional.of(4105L));

        service.upsertAndRecord(10L, chatId, sku,
                "NEW TITLE", 4105L, ProductAvailability.OUT_OF_STOCK);

        // сохранили обновлённый
        ArgumentCaptor<TrackedItem> itemCap = ArgumentCaptor.forClass(TrackedItem.class);
        verify(repo).save(itemCap.capture());
        TrackedItem saved = itemCap.getValue();

        assertThat(saved.getTitle()).isEqualTo("NEW TITLE");
        assertThat(saved.getAvailability()).isEqualTo(ProductAvailability.OUT_OF_STOCK);
        // history.append НЕ вызывается, цена такая же
        verify(history, never()).append(eq(sku), anyLong(), any());
    }

    @Test
    void upsertAndRecord_existing_priceChanged_writesHistory() {
        Long chatId = 20L, sku = 291003311L;
        TrackedItem existing = TrackedItem.builder()
                .chatId(chatId)
                .sku(sku)
                .title("OLD")
                .lastPrice(5000L)
                .availability(ProductAvailability.AVAILABLE)
                .build();
        when(repo.findByChatIdAndSku(chatId, sku)).thenReturn(Optional.of(existing));
        // в истории была 5000
        when(history.getLastPrice(sku)).thenReturn(Optional.of(5000L));

        service.upsertAndRecord(10L, chatId, sku,
                "OLD", 4105L, ProductAvailability.AVAILABLE);

        verify(history).append(sku, 4105L, ProductAvailability.AVAILABLE);
        // и lastPrice обновили
        ArgumentCaptor<TrackedItem> itemCap = ArgumentCaptor.forClass(TrackedItem.class);
        verify(repo).save(itemCap.capture());
        assertThat(itemCap.getValue().getLastPrice()).isEqualTo(4105L);
    }

    @Test
    void setThreshold_noItem_createsNewWithThreshold() {
        Long chatId = 20L, userId = 10L, sku = 1L;
        when(repo.findByChatIdAndSku(chatId, sku)).thenReturn(Optional.empty());

        service.setThreshold(chatId, userId, sku, ThresholdType.PERCENT, 15L);

        ArgumentCaptor<TrackedItem> itemCap = ArgumentCaptor.forClass(TrackedItem.class);
        verify(repo).save(itemCap.capture());
        TrackedItem saved = itemCap.getValue();

        assertThat(saved.getChatId()).isEqualTo(chatId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getSku()).isEqualTo(sku);
        assertThat(saved.getThresholdType()).isEqualTo(ThresholdType.PERCENT);
        assertThat(saved.getThresholdValue()).isEqualTo(15L);
        assertThat(saved.getAvailability()).isEqualTo(ProductAvailability.UNKNOWN);
    }

    @Test
    void setThreshold_existing_updates() {
        Long chatId = 20L, userId = 10L, sku = 1L;
        TrackedItem existing = TrackedItem.builder()
                .chatId(chatId)
                .userId(userId)
                .sku(sku)
                .thresholdType(ThresholdType.PERCENT)
                .thresholdValue(10L)
                .build();
        when(repo.findByChatIdAndSku(chatId, sku)).thenReturn(Optional.of(existing));

        service.setThreshold(chatId, userId, sku, ThresholdType.PRICE, 4990L);

        ArgumentCaptor<TrackedItem> itemCap = ArgumentCaptor.forClass(TrackedItem.class);
        verify(repo).save(itemCap.capture());
        TrackedItem saved = itemCap.getValue();

        assertThat(saved.getThresholdType()).isEqualTo(ThresholdType.PRICE);
        assertThat(saved.getThresholdValue()).isEqualTo(4990L);
    }

    @Test
    void removeTracked_delegatesToRepo() {
        Long chatId = 20L, sku = 1L;
        TrackedItem ti = TrackedItem.builder().chatId(chatId).sku(sku).build();
        when(repo.deleteByChatIdAndSku(chatId, sku)).thenReturn(Optional.of(ti));

        var opt = service.removeTracked(chatId, sku);

        assertThat(opt).isPresent();
        assertThat(opt.get().getSku()).isEqualTo(sku);
        verify(repo).deleteByChatIdAndSku(chatId, sku);
    }
}
