package ru.maelnor.ozonbomgebot.bot.queue;

public interface JobQueue {
    void enqueue(QueueJob job);
}