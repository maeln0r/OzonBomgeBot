package ru.maelnor.ozonbomgebot.bot.command;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.maelnor.ozonbomgebot.bot.entity.TrackedItem;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class ListCommandTest {

    @Test
    void execute_whenEmpty_sendsEmptyMessage() {
        FlowIO io = mock(FlowIO.class);
        TrackedItemRepository repo = mock(TrackedItemRepository.class);
        when(repo.findByChat(123L)).thenReturn(List.of());

        ListCommand cmd = new ListCommand(io, repo);

        Update u = new Update();
        Message m = new Message();
        m.setChat(new Chat(123L, "private"));
        u.setMessage(m);

        cmd.execute(u);

        verify(io).toast(123L, "Список пуст. Добавь товар командой: /add_ozon");
    }

    @Test
    void execute_whenHasItems_formatsAndSends() {
        FlowIO io = mock(FlowIO.class);
        TrackedItemRepository repo = mock(TrackedItemRepository.class);

        TrackedItem item = TrackedItem.builder()
                .id(1)
                .chatId(123L)
                .sku(291003311L)
                .title("Тестовый товар")
                .startPrice(5000L)
                .lastPrice(4500L)
                .availability(ProductAvailability.AVAILABLE)
                .thresholdType(ThresholdType.PRICE)
                .thresholdValue(4200L)
                .createdAtMs(1L)
                .updatedAtMs(1L)
                .build();

        when(repo.findByChat(123L)).thenReturn(List.of(item));

        ListCommand cmd = new ListCommand(io, repo);

        Update u = new Update();
        Message m = new Message();
        m.setChat(new Chat(123L, "private"));
        u.setMessage(m);

        cmd.execute(u);

        // проверяем, что что-то внятное отправили
        verify(io, atLeastOnce()).toast(eq(123L), contains("Отслеживаемые товары"));
        verify(io, atLeastOnce()).toast(eq(123L), contains("Тестовый товар"));
    }
}
