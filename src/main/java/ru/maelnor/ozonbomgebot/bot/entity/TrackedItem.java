package ru.maelnor.ozonbomgebot.bot.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.model.ThresholdType;

@Entity
@Table(name = "tracked_item",
        uniqueConstraints = @UniqueConstraint(name = "ux_tracked_item_chat_sku",
                columnNames = {"chat_id", "sku"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "sku", nullable = false)
    private Long sku;

    private String title;

    @Column(name = "start_price")
    private Long startPrice;
    @Column(name = "last_price")
    private Long lastPrice;   // кэш последней цены из истории

    @Enumerated(EnumType.STRING)
    private ProductAvailability availability;

    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_type")
    private ThresholdType thresholdType;

    @Column(name = "threshold_value")
    private Long thresholdValue;


    @Column(name = "created_at_ms", nullable = false)
    private Long createdAtMs;
    @Column(name = "updated_at_ms", nullable = false)
    private Long updatedAtMs;
}