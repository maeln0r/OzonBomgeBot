package ru.maelnor.ozonbomgebot.bot.service.integration.ozon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.maelnor.ozonbomgebot.bot.model.ProductAvailability;
import ru.maelnor.ozonbomgebot.bot.model.ProductInfo;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Парсер composer JSON -> ProductInfo.
 * Реализованы два флоу:
 * 1) OUT_OF_STOCK: ищем webOutOfStock строго по цепочке контейнеров, берем skuName/sku из соответствующего state
 * 2) AVAILABLE: webProductHeading.title + webPrice.cardPrice + webProductMainWidget.sku (по глобальному поиску по layout)
 */
@Slf4j
@Component
public class OzonProductParser {

    private final ObjectMapper om = new ObjectMapper();

    public Optional<ProductInfo> parse(String composerJson) {
        if (composerJson == null || composerJson.isBlank()) return Optional.empty();
        final JsonNode root;
        try {
            root = om.readTree(composerJson);
        } catch (IOException e) {
            log.warn("parse(): invalid JSON root: {}", e.getMessage());
            return Optional.empty();
        }

        // 1) Попытка OUT_OF_STOCK флоу строго по пути контейнеров
        try {
            Optional<ProductInfo> oos = parseOutOfStock(root);
            if (oos.isPresent()) return oos;
        } catch (Exception e) {
            log.debug("parse(): out-of-stock flow failed: {}", e.getMessage());
        }

        // 2) Попытка AVAILABLE флоу
        return parseAvailable(root);

    }

    /* -------------------------------------------------------------- */
    /*                         OUT-OF-STOCK FLOW                       */
    /* -------------------------------------------------------------- */

    private Optional<ProductInfo> parseOutOfStock(JsonNode root) {
        JsonNode layout = root.path("layout");
        if (layout.isMissingNode()) return Optional.empty();

        List<JsonNode> level = asList(layout);
        List<JsonNode> wallpapers = level.stream()
                .filter(n -> "wallpaper".equals(componentOf(n)))
                .collect(Collectors.toList());
        if (wallpapers.isEmpty()) return Optional.empty();

        List<JsonNode> containerLevel = descendByComponent(wallpapers, "container");
        if (containerLevel.isEmpty()) return Optional.empty();

        JsonNode outOfStockWidget = findFirstByComponent(containerLevel, "webOutOfStock");
        if (outOfStockWidget == null) return Optional.empty();

        String stateId = text(outOfStockWidget, "stateId");
        if (stateId == null || stateId.isBlank()) return Optional.empty();

        JsonNode widgetStates = root.path("widgetStates");
        if (!widgetStates.isObject()) return Optional.empty();
        JsonNode stateJsonStringNode = widgetStates.get(stateId);
        if (stateJsonStringNode == null || !stateJsonStringNode.isTextual()) return Optional.empty();

        String stateJsonText = stateJsonStringNode.asText();
        try {
            JsonNode state = om.readTree(stateJsonText);
            String skuStr = text(state, "sku");
            String skuName = text(state, "skuName");
            if (skuStr == null || skuStr.isBlank() || skuName == null || skuName.isBlank()) return Optional.empty();

            long sku = Long.parseLong(skuStr);
            ProductInfo info = new ProductInfo(skuName, sku, 0L, ProductAvailability.OUT_OF_STOCK);
            return Optional.of(info);
        } catch (Exception e) {
            log.warn("parseOutOfStock(): cannot parse state JSON for {}: {}", stateId, e.getMessage());
            return Optional.empty();
        }
    }

    /* -------------------------------------------------------------- */
    /*                          AVAILABLE FLOW                          */
    /* -------------------------------------------------------------- */

    private Optional<ProductInfo> parseAvailable(JsonNode root) {
        // В доступном флоу виджеты могут лежать в разных ветках layout, поэтому делаем ГЛОБАЛЬНЫЙ поиск по всему layout
        // и по найденным stateId вытягиваем данные из widgetStates.

        // 1) webProductHeading -> title
        String title = null;
        String titleStateId = findFirstStateIdGlobal(root, "webProductHeading");
        if (titleStateId != null) {
            JsonNode st = getWidgetState(root, titleStateId);
            if (st != null) title = text(st, "title");
        }

        // 2) webPrice -> cardPrice -> long руб
        Long price = null;
        String priceStateId = findFirstStateIdGlobal(root, "webPrice");
        if (priceStateId != null) {
            JsonNode st = getWidgetState(root, priceStateId);
            if (st != null) {
                String cardPrice = text(st, "cardPrice");
                if (cardPrice != null) {
                    price = parsePrice(cardPrice);
                } else {
                    String commonPrice = text(st, "price");
                    if (commonPrice != null) price = parsePrice(commonPrice);
                }
            }
        }

        // 3) webProductMainWidget -> sku
        Long sku = null;
        String skuStateId = findFirstStateIdGlobal(root, "webProductMainWidget");
        if (skuStateId != null) {
            JsonNode st = getWidgetState(root, skuStateId);
            if (st != null) {
                String skuStr = text(st, "sku");
                if (skuStr != null && !skuStr.isBlank()) {
                    try {
                        sku = Long.parseLong(skuStr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        if (title != null && sku != null && price != null) {
            return Optional.of(new ProductInfo(title, sku, price, ProductAvailability.AVAILABLE));
        }
        return Optional.empty();
    }

    /* -------------------------------------------------------------- */
    /*                             Helpers                              */
    /* -------------------------------------------------------------- */

    private static String componentOf(JsonNode node) {
        return node != null && node.has("component") ? node.get("component").asText(null) : null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node != null ? node.get(field) : null;
        return v != null && v.isTextual() ? v.asText() : null;
    }

    private static List<JsonNode> asList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return List.of();
        if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            List<JsonNode> out = new ArrayList<>(arr.size());
            arr.forEach(out::add);
            return out;
        }
        return List.of(node);
    }

    private JsonNode getWidgetState(JsonNode root, String stateId) {
        if (stateId == null) return null;
        JsonNode ws = root.path("widgetStates");
        if (!ws.isObject()) return null;
        JsonNode val = ws.get(stateId);
        if (val == null || !val.isTextual()) return null;
        try {
            return om.readTree(val.asText());
        } catch (IOException e) {
            log.debug("getWidgetState(): cannot parse state {}: {}", stateId, e.getMessage());
            return null;
        }
    }

    private static String findFirstStateId(List<JsonNode> containers, String component) {
        JsonNode w = findFirstByComponent(containers, component);
        if (w == null) return null;
        String sid = w.has("stateId") && w.get("stateId").isTextual() ? w.get("stateId").asText() : null;
        return (sid != null && !sid.isBlank()) ? sid : null;
    }

    /**
     * Глобальный поиск первого stateId виджета с указанным component по всему layout.
     */
    private static String findFirstStateIdGlobal(JsonNode root, String component) {
        JsonNode layout = root.path("layout");
        Deque<JsonNode> dq = new ArrayDeque<>();
        if (layout.isArray()) layout.forEach(dq::addLast);
        else dq.addLast(layout);
        while (!dq.isEmpty()) {
            JsonNode n = dq.removeFirst();
            if (component.equals(componentOf(n))) {
                JsonNode sidNode = n.get("stateId");
                if (sidNode != null && sidNode.isTextual()) return sidNode.asText();
            }
            for (JsonNode ph : asList(n.path("placeholders"))) {
                for (JsonNode w : asList(ph.path("widgets"))) {
                    dq.addLast(w);
                }
            }
        }
        return null;
    }

    private static Long parsePrice(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Возвращает первый встретившийся widget с заданным component среди потомков текущего уровня.
     */
    private static JsonNode findFirstByComponent(List<JsonNode> currentLevel, String targetComponent) {
        Deque<JsonNode> dq = new ArrayDeque<>(currentLevel);
        while (!dq.isEmpty()) {
            JsonNode n = dq.removeFirst();
            if (targetComponent.equals(componentOf(n))) return n;
            // обход в ширину по placeholders/widgets
            for (JsonNode ph : asList(n.path("placeholders"))) {
                for (JsonNode w : asList(ph.path("widgets"))) {
                    dq.addLast(w);
                }
            }
        }
        return null;
    }

    /**
     * Спускается с текущих узлов по placeholders -> widgets и выбирает все узлы,
     * у которых component == targetComponent. Поддерживает вложенные контейнеры.
     */
    private static List<JsonNode> descendByComponent(List<JsonNode> currentLevel, String targetComponent) {
        List<JsonNode> next = new ArrayList<>();
        Deque<JsonNode> dq = new ArrayDeque<>(currentLevel);
        while (!dq.isEmpty()) {
            JsonNode n = dq.removeFirst();
            for (JsonNode ph : asList(n.path("placeholders"))) {
                for (JsonNode w : asList(ph.path("widgets"))) {
                    if (targetComponent.equals(componentOf(w))) {
                        next.add(w);
                    }
                    // продолжаем обход в глубину - контейнеры могут быть вложенными
                    dq.addLast(w);
                }
            }
        }
        return next;
    }
}
