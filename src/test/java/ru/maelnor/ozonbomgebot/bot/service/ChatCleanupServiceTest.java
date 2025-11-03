package ru.maelnor.ozonbomgebot.bot.service;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;
import ru.maelnor.ozonbomgebot.bot.security.AdminAccessService;

import static org.mockito.Mockito.*;

class ChatCleanupServiceTest {

    private final TrackedItemRepository trackedRepo = mock(TrackedItemRepository.class);
    private final AdminAccessService adminAccessService = mock(AdminAccessService.class);
    private final ChatCleanupService service = new ChatCleanupService(trackedRepo, adminAccessService);

    @Test
    void cleanupChat_nullChatId_doesNothing() {
        service.cleanupChat(null, "reason");

        verifyNoInteractions(trackedRepo, adminAccessService);
    }

    @Test
    void cleanupChat_deletesTrackedAndInvalidatesAdminCache() {
        when(trackedRepo.count()).thenReturn(10L, 3L); // before, after

        service.cleanupChat(123L, "bot removed from chat");

        verify(trackedRepo).deleteAllByChatId(123L);
        verify(adminAccessService).invalidateChat(123L);
    }
}
