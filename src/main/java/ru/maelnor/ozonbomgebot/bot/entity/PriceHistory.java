package ru.maelnor.ozonbomgebot.bot.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;

@Entity
@Table(name = "price_history",
        indexes = @Index(name = "ix_price_history_sku_time", columnList = "sku,created_at_ms"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Long sku;
    @Column(nullable = false)
    private Long price;
    @Column(name = "created_at_ms", nullable = false)
    private Long createdAtMs;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductAvailability availability;
}