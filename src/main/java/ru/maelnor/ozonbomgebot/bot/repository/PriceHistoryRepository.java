package ru.maelnor.ozonbomgebot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.maelnor.ozonbomgebot.bot.entity.PriceHistory;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;

import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Integer> {
    @Query("""
            select ph from PriceHistory ph
            where ph.sku = :sku
            order by ph.createdAtMs desc
            """)
    Optional<PriceHistory> findLastBySku(@Param("sku") Long sku);

    Optional<PriceHistory> findTopBySkuOrderByCreatedAtMsDesc(long sku);

    Optional<PriceHistory> findTopBySkuAndAvailabilityOrderByCreatedAtMsDesc(long sku, ProductAvailability availability);

    @Query("""
            select ph from PriceHistory ph
            where ph.sku = :sku
            order by ph.createdAtMs asc
            """)
    List<PriceHistory> findAllBySkuOrderByCreatedAtMsAsc(@Param("sku") long sku);
}