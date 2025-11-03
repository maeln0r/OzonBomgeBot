package ru.maelnor.ozonbomgebot.bot.flow.util;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeyboardUtilTest {

    @Test
    void backCancelKb_buildsKeyboardWithTwoButtonsInOneRow() {
        FlowIO.Keyboard kb = KeyboardUtil.backCancelKb("add_ozon");

        // одна строка
        List<List<FlowIO.Button>> rows = kb.rows();
        assertThat(rows).hasSize(1);

        List<FlowIO.Button> row = rows.get(0);
        assertThat(row).hasSize(2);

        FlowIO.Button back = row.get(0);
        FlowIO.Button cancel = row.get(1);

        assertThat(back.text()).isEqualTo("⬅️ Назад");
        assertThat(back.data()).isEqualTo("add_ozon:back");

        assertThat(cancel.text()).isEqualTo("Отмена");
        assertThat(cancel.data()).isEqualTo("add_ozon:cancel");
    }
}
