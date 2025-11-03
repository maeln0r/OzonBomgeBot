package ru.maelnor.ozonbomgebot.bot.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class InMemoryJobQueue implements JobQueue {
    private final BlockingQueue<QueueJob> q = new LinkedBlockingQueue<>();

    @Override
    public void enqueue(QueueJob job) {
        if (job == null) return;
        q.offer(job);
        log.debug("job enqueued: {}", job);
    }

    public BlockingQueue<QueueJob> backingQueue() {
        return q;
    }
}