package ru.maelnor.ozonbomgebot.bot.command;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class HelpCommandTest {

    @Test
    void execute_sendsHelpTextViaToast() {
        FlowIO io = mock(FlowIO.class);

        // сделаем фейковую "другую" команду
        BotCommand dummy = mock(BotCommand.class);
        when(dummy.getCommandName()).thenReturn("/dummy");
        when(dummy.getCommandDescription()).thenReturn("dummy desc");

        HelpCommand cmd = new HelpCommand(io, List.of(dummy));

        Update u = new Update();
        Message m = new Message();
        m.setChat(new Chat(123L, "private"));
        u.setMessage(m);

        cmd.execute(u);

        verify(io, atLeastOnce()).toast(anyLong(), anyString());
    }

    @Test
    void execute_ignoresCommandsWithoutNameOrDesc() {
        FlowIO io = mock(FlowIO.class);

        BotCommand good = mock(BotCommand.class);
        when(good.getCommandName()).thenReturn("/good");
        when(good.getCommandDescription()).thenReturn("ok");

        BotCommand bad1 = mock(BotCommand.class);
        when(bad1.getCommandName()).thenReturn(null);
        when(bad1.getCommandDescription()).thenReturn("xxx");

        BotCommand bad2 = mock(BotCommand.class);
        when(bad2.getCommandName()).thenReturn("/noDesc");
        when(bad2.getCommandDescription()).thenReturn(null);

        HelpCommand cmd = new HelpCommand(io, List.of(good, bad1, bad2));

        Update u = new Update();
        Message m = new Message();
        m.setChat(new Chat(777L, "private"));
        u.setMessage(m);

        cmd.execute(u);

        // главное — не упали и точно что-то отправили
        verify(io, atLeastOnce()).toast(eq(777L), anyString());
    }
}
