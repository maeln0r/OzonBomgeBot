package ru.maelnor.ozonbomgebot.bot.flow;

import java.util.Optional;

public interface FlowRegistry {
    Optional<Flow> create(String flowId);
}
