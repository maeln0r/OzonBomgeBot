package ru.maelnor.ozonbomgebot.bot.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccessService {
    private final TelegramClient telegramClient;

    // простой TTL-кэш на 5 минут, чтобы не долбить Telegram API
    private static final long TTL_MS = 5 * 60 * 1000L;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(boolean isAdmin, long ts) {
    }

    /**
     * Можно ли выполнять админскую команду в контексте апдейта
     */
    public boolean isAdmin(Update update) {
        Long chatId = extractChatId(update).orElse(null);
        Long userId = extractUserId(update).orElse(null);
        if (chatId == null || userId == null) return false;

        // Личка - пользователь сам себе админ
        if (isPrivate(update)) return true;

        // Кэш для групп
        String key = chatId + ":" + userId;
        CacheEntry ce = cache.get(key);
        long now = System.currentTimeMillis();
        if (ce != null && now - ce.ts < TTL_MS) {
            return ce.isAdmin;
        }

        boolean admin = fetchIsAdminFromTelegram(chatId, userId);
        cache.put(key, new CacheEntry(admin, now));
        return admin;
    }

    private boolean fetchIsAdminFromTelegram(Long chatId, Long userId) {
        try {
            ChatMember cm = telegramClient.execute(new GetChatMember(chatId.toString(), userId));
            // админ - владелец или администратор
            boolean isOwner = cm instanceof ChatMemberOwner;
            boolean isAdmin = cm instanceof ChatMemberAdministrator;
            return isOwner || isAdmin;
        } catch (TelegramApiException e) {
            log.warn("getChatMember failed chatId={} userId={}, err={}", chatId, userId, e.getMessage());
            return false;
        }
    }

    private static Optional<Long> extractChatId(Update u) {
        if (u == null) return Optional.empty();
        if (u.getMessage() != null && u.getMessage().getChat() != null)
            return Optional.of(u.getMessage().getChat().getId());
        if (u.getCallbackQuery() != null && u.getCallbackQuery().getMessage() != null)
            return Optional.ofNullable(u.getCallbackQuery().getMessage().getChatId());
        if (u.getMyChatMember() != null && u.getMyChatMember().getChat() != null)
            return Optional.of(u.getMyChatMember().getChat().getId());
        if (u.getChatMember() != null && u.getChatMember().getChat() != null)
            return Optional.of(u.getChatMember().getChat().getId());
        return Optional.empty();
    }

    private static Optional<Long> extractUserId(Update u) {
        if (u == null) return Optional.empty();
        User from = null;
        if (u.getMessage() != null) from = u.getMessage().getFrom();
        else if (u.getCallbackQuery() != null) from = u.getCallbackQuery().getFrom();
        else if (u.getMyChatMember() != null) from = u.getMyChatMember().getFrom();
        else if (u.getChatMember() != null) from = u.getChatMember().getFrom();
        return Optional.ofNullable(from).map(User::getId).map(Long::valueOf);
    }

    private static boolean isPrivate(Update u) {
        var msg = u.getMessage();
        if (msg != null && msg.getChat() != null) {
            return Objects.equals("private", msg.getChat().getType());
        }
        var cb = u.getCallbackQuery();
        if (cb != null && cb.getMessage() != null && cb.getMessage().getChat() != null) {
            return Objects.equals("private", cb.getMessage().getChat().getType());
        }
        return false;
    }

    /**
     * Можно дергать при событиях my_chat_member/left/kicked - чистит кэш по чату
     */
    public void invalidateChat(Long chatId) {
        if (chatId == null) return;
        String prefix = chatId + ":";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Периодическая чистка протухших записей
     */
    public void cleanupExpired() {
        long now = Instant.now().toEpochMilli();
        cache.entrySet().removeIf(e -> now - e.getValue().ts >= TTL_MS);
    }
}
