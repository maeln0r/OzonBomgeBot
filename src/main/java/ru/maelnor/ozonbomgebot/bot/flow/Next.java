package ru.maelnor.ozonbomgebot.bot.flow;

public sealed interface Next permits Next.Stay, Next.Goto, Next.Done, Next.Cancel {
    record Stay() implements Next {
    }

    record Goto(String stateId) implements Next {
    }

    record Done() implements Next {
    }

    record Cancel(String reason) implements Next {
    }
}
