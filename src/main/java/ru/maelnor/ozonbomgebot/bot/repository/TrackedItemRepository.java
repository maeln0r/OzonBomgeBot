package ru.maelnor.ozonbomgebot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.maelnor.ozonbomgebot.bot.entity.TrackedItem;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;

import java.util.List;
import java.util.Optional;

public interface TrackedItemRepository extends JpaRepository<TrackedItem, Integer> {
    @Query("select t from TrackedItem t where t.chatId = :chatId order by t.createdAtMs asc")
    List<TrackedItem> findByChat(@Param("chatId") Long chatId);

    @Query("select t from TrackedItem t where t.sku = :sku")
    List<TrackedItem> findTrackedItemsBySku(@Param("sku") Long sku);

    Optional<TrackedItem> findByChatIdAndSku(Long chatId, Long sku);

    void deleteAllByChatId(Long chatId);

    /**
     * Удаляет одну запись по (chatId, sku). История цен не затрагивается.
     * Возвращает количество удаленных строк.
     */
    Optional<TrackedItem> deleteByChatIdAndSku(Long chatId, Long sku);

    @Modifying
    @Query(value = """
            insert into tracked_item (chat_id, user_id, sku, title, start_price, last_price, availability, created_at_ms, updated_at_ms)
            values (:chatId,:userId,:sku,:title,:startPrice,:lastPrice,:availability,:createdAt,:updatedAt)
            on conflict(chat_id, sku) do update set
              title=excluded.title,
              last_price=excluded.last_price,
              availability=excluded.availability,
              updated_at_ms=excluded.updated_at_ms
            """, nativeQuery = true)
    int upsert(@Param("chatId") Long chatId,
               @Param("userId") Long userId,
               @Param("sku") Long sku,
               @Param("title") String title,
               @Param("startPrice") Long startPrice,
               @Param("lastPrice") Long lastPrice,
               @Param("availability") ProductAvailability availability,
               @Param("createdAt") Long createdAt,
               @Param("updatedAt") Long updatedAt);

    @Query("""
        select t.sku
        from TrackedItem t
        where t.availability <> :excluded
        group by t.sku
        order by count(t) desc
        """)
    List<Long> findSkuOrderByPopularityExcludingAvailability(@Param("excluded") ProductAvailability excluded);
}