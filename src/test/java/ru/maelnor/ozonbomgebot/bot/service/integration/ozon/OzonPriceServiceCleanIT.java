package ru.maelnor.ozonbomgebot.bot.service.integration.ozon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.model.ProductInfo;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class OzonPriceServiceCleanIT {

    private final OzonProductParser parser = new OzonProductParser();
    private final OzonPriceServiceClean service = new OzonPriceServiceClean(parser);

    @AfterEach
    void tearDown() {
        // важно закрыть браузер/контекст, сервис у нас DisposableBean
        service.destroy();
    }

    @Test
    void fetch_realSku_success() throws Exception {
        // можно положить сюда несколько «хороших» SKU,
        // чтобы если один внезапно умер/пропал — тест попробовал следующий.
        long[] candidates = {
                291003311L,
                1693902088L,
                2806520922L,
                1748829320L
        };

        IOException lastIo = null;
        for (long sku : candidates) {
            try {
                ProductInfo info = service.fetch(sku);
                assertNotNull(info, "fetch() вернул null для sku=" + sku);
                assertEquals(sku, info.sku(), "SKU в ответе не совпал с запрошенным");
                assertNotNull(info.title(), "title пустой для sku=" + sku);

                // price у OOS может быть 0, поэтому только проверим, что не упали
                System.out.println("OK: " + info);
                return; // успех — тест можно завершать
            } catch (IOException e) {
                // могли упасть на antibot / 307 без Location / что-то еще из fetch()
                lastIo = e;
                System.err.println("SKU " + sku + " не удалось вытащить: " + e.getMessage());
            } catch (InterruptedException e) {
                // если нас прервали — прерываем и тест
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        // если сюда дошли — ни один SKU не сработал
        fail("Не удалось получить ни один SKU из списка, последняя ошибка: " + lastIo.getMessage());
    }
}
