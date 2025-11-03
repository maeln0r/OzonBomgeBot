package ru.maelnor.ozonbomgebot.bot.service.integration.ozon;

import ru.maelnor.ozonbomgebot.bot.model.ProductInfo;

import java.io.IOException;

public interface OzonPriceService {
    ProductInfo fetch(long sku) throws IOException, InterruptedException;
}
