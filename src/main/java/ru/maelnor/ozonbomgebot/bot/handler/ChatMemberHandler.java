package ru.maelnor.ozonbomgebot.bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;
import ru.maelnor.ozonbomgebot.bot.service.ChatCleanupService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemberHandler {
    private final ChatCleanupService cleanupService;

    public void handle(Update update) {
        ChatMemberUpdated mc = update.getMyChatMember();
        if (mc == null) return;

        Long chatId = mc.getChat() != null ? mc.getChat().getId() : null;
        String chatType = mc.getChat() != null ? mc.getChat().getType() : "unknown";
        String oldStatus = mc.getOldChatMember() != null ? mc.getOldChatMember().getStatus() : "unknown";
        String newStatus = mc.getNewChatMember() != null ? mc.getNewChatMember().getStatus() : "unknown";

        log.info("my_chat_member: chatId={}, type={}, {} -> {}", chatId, chatType, oldStatus, newStatus);

        // если бот больше не участник чата - чистим
        if (!isBotActive(newStatus)) {
            String reason = chatType + " status " + oldStatus + " -> " + newStatus;
            cleanupService.cleanupChat(chatId, reason);
        }
    }

    /**
     * Активные статусы бота в чате
     */
    private boolean isBotActive(String status) {
        if (status == null) return false;
        return switch (status) {
            case "creator", "administrator", "member" -> true;
            default -> false;
        };
    }
}
