package ru.maelnor.ozonbomgebot.bot.command;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AbstractBotCommandTest {

    static class TestCommand extends AbstractBotCommand {
        public TestCommand(FlowIO io) {
            super(io, "/test", "desc", false);
            // костыльно в тестах: руками проставим имя бота
            try {
                var f = AbstractBotCommand.class.getDeclaredField("botName");
                f.setAccessible(true);
                f.set(this, "MyBot");
            } catch (Exception ignored) {
            }
        }

        @Override
        public void execute(org.telegram.telegrambots.meta.api.objects.Update update) {
        }
    }

    @Test
    void supports_plainCommand() {
        FlowIO io = mock(FlowIO.class);
        TestCommand cmd = new TestCommand(io);

        assertTrue(cmd.supports("/test"));
        assertTrue(cmd.supports("/test 123"));
        assertFalse(cmd.supports("/other"));
    }

    @Test
    void supports_commandWithMention() {
        FlowIO io = mock(FlowIO.class);
        TestCommand cmd = new TestCommand(io);

        assertTrue(cmd.supports("/test@MyBot"));
        assertTrue(cmd.supports("/test@MyBot more"));
        assertFalse(cmd.supports("/test@OtherBot"));
    }
}
