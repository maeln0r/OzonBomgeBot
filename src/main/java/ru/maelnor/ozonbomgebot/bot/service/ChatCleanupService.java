package ru.maelnor.ozonbomgebot.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;
import ru.maelnor.ozonbomgebot.bot.security.AdminAccessService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCleanupService {
    private final TrackedItemRepository trackedRepo;
    private final AdminAccessService adminAccessService; // чтобы инвалидировать кэш прав

    /**
     * Полная очистка данных, привязанных к чату.
     * История цен (price_history) - НЕ трогаем (глобальная по SKU).
     */
    @Transactional
    public void cleanupChat(Long chatId, String reason) {
        if (chatId == null) return;
        long before = trackedRepo.count();
        trackedRepo.deleteAllByChatId(chatId);
        adminAccessService.invalidateChat(chatId);

        long after = trackedRepo.count();
        long diff = before - after;
        log.info("Chat cleanup: chatId={}, removed tracked_items ~{}, reason={}", chatId, diff, reason);
    }
}
