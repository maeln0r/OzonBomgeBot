package ru.maelnor.ozonbomgebot.bot.flow.util;

import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;

import static ru.maelnor.ozonbomgebot.bot.flow.util.FlowUtil.cb;

public class KeyboardUtil {
    public static FlowIO.Keyboard backCancelKb(String flow_id) {
        return FlowIO.Keyboard.builder()
                .row(new FlowIO.Button("⬅️ Назад", cb(flow_id, "back")), new FlowIO.Button("Отмена", cb(flow_id, "cancel")))
                .build();
    }
}
