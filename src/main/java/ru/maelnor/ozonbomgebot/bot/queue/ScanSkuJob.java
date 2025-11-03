package ru.maelnor.ozonbomgebot.bot.queue;

public record ScanSkuJob(Long sku, Long chatId, Long userId, String reason) implements QueueJob {
}