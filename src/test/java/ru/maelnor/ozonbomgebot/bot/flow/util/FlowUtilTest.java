package ru.maelnor.ozonbomgebot.bot.flow.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowUtilTest {

    @Test
    void cb_concatenatesFlowIdAndTailWithColon() {
        String cb = FlowUtil.cb("add_ozon", "cancel");

        assertThat(cb).isEqualTo("add_ozon:cancel");
    }

    @Test
    void cb_worksWithEmptyTail() {
        String cb = FlowUtil.cb("flow", "");

        assertThat(cb).isEqualTo("flow:");
    }

    @Test
    void cb_worksWithEmptyFlowId() {
        String cb = FlowUtil.cb("", "tail");

        assertThat(cb).isEqualTo(":tail");
    }
}
