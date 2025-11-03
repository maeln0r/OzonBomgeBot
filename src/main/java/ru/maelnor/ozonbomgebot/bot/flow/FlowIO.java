package ru.maelnor.ozonbomgebot.bot.flow;

import java.util.ArrayList;
import java.util.List;

public interface FlowIO {
    Integer send(long chatId, String text, Keyboard kb);

    void edit(long chatId, Integer messageId, String text, Keyboard kb);

    void toast(long chatId, String text);

    default void sendPhoto(long chatId, java.io.File file, String caption) {
        throw new UnsupportedOperationException("Photos not supported");
    }

    record Keyboard(List<List<Button>> rows) {
        public static Keyboard of(Button... buttons) {
            return new Keyboard(List.of(List.of(buttons)));
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final List<List<Button>> rows = new ArrayList<>();

            public Builder row(Button... buttons) {
                rows.add(List.of(buttons));
                return this;
            }

            public Keyboard build() {
                return new Keyboard(rows);
            }
        }
    }

    record Button(String text, String data) {
    }
}
