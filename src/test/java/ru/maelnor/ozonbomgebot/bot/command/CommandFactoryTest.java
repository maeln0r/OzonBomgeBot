package ru.maelnor.ozonbomgebot.bot.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandFactoryTest {

    @Test
    void getCommand_whenOneSupports_returnsIt() {
        BotCommand c1 = mock(BotCommand.class);
        when(c1.supports("/a")).thenReturn(false);

        BotCommand c2 = mock(BotCommand.class);
        when(c2.supports("/a")).thenReturn(true);

        CommandFactory f = new CommandFactory(List.of(c1, c2));

        Optional<BotCommand> cmd = f.getCommand("/a");

        assertTrue(cmd.isPresent());
        assertEquals(c2, cmd.get());
    }

    @Test
    void getCommand_whenNoneSupports_returnsEmpty() {
        BotCommand c1 = mock(BotCommand.class);
        when(c1.supports("/a")).thenReturn(false);

        CommandFactory f = new CommandFactory(List.of(c1));

        Optional<BotCommand> cmd = f.getCommand("/a");

        assertTrue(cmd.isEmpty());
    }
}
