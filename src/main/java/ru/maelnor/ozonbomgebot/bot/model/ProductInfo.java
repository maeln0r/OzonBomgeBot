package ru.maelnor.ozonbomgebot.bot.model;


public record ProductInfo(String title, long sku, long price, ProductAvailability availability) {
}