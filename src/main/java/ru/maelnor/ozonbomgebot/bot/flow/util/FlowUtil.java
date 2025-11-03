package ru.maelnor.ozonbomgebot.bot.flow.util;

public class FlowUtil {

    public static String cb(String flow_id, String tail) {
        return flow_id + ":" + tail;
    }
}
