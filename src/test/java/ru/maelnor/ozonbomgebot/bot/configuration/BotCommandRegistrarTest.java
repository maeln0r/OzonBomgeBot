package ru.maelnor.ozonbomgebot.bot.configuration;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

class BotCommandRegistrarTest {

    @Test
    void registerCommands_sendsOnlyValidOnes_andHelpFirst() throws Exception {
        TelegramClient client = mock(TelegramClient.class);

        ru.maelnor.ozonbomgebot.bot.command.BotCommand help =
                mock(ru.maelnor.ozonbomgebot.bot.command.BotCommand.class);
        when(help.getCommandName()).thenReturn("/help");
        when(help.getCommandDescription()).thenReturn("помощь");

        ru.maelnor.ozonbomgebot.bot.command.BotCommand add =
                mock(ru.maelnor.ozonbomgebot.bot.command.BotCommand.class);
        when(add.getCommandName()).thenReturn("/add_ozon");
        when(add.getCommandDescription()).thenReturn("добавить");

        // сломанная команда — должна отфильтроваться
        ru.maelnor.ozonbomgebot.bot.command.BotCommand broken =
                mock(ru.maelnor.ozonbomgebot.bot.command.BotCommand.class);
        when(broken.getCommandName()).thenReturn(null);

        BotCommandRegistrar reg = new BotCommandRegistrar(
                client,
                List.of(help, add, broken)
        );

        reg.registerCommands();

        // забираем SetMyCommands, который ушёл в Telegram
        var captor = org.mockito.ArgumentCaptor.forClass(SetMyCommands.class);
        verify(client).execute(captor.capture());

        SetMyCommands smc = captor.getValue();
        List<BotCommand> cmds = smc.getCommands();

        // должно быть только 2: help и add, и help — первый
        org.assertj.core.api.Assertions.assertThat(cmds)
                .hasSize(2)
                .extracting(BotCommand::getCommand)
                .containsExactly("/help", "/add_ozon");
    }

    @Test
    void registerCommands_telegramThrows_isPropagated() throws Exception {
        TelegramClient client = mock(TelegramClient.class);
        when(client.execute(isA(SetMyCommands.class)))
                .thenThrow(new RuntimeException("tg"));

        ru.maelnor.ozonbomgebot.bot.command.BotCommand help =
                mock(ru.maelnor.ozonbomgebot.bot.command.BotCommand.class);
        when(help.getCommandName()).thenReturn("/help");
        when(help.getCommandDescription()).thenReturn("help");

        BotCommandRegistrar reg = new BotCommandRegistrar(client, List.of(help));

        assertThatThrownBy(reg::registerCommands)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("tg");

        verify(client).execute(isA(SetMyCommands.class));
    }
}
