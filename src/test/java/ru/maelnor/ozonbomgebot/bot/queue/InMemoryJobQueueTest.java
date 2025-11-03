package ru.maelnor.ozonbomgebot.bot.queue;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryJobQueueTest {

    @Test
    void enqueue_null_isIgnored() {
        InMemoryJobQueue q = new InMemoryJobQueue();

        q.enqueue(null);

        assertThat(q.backingQueue()).isEmpty();
    }

    @Test
    void enqueue_job_isAddedToBackingQueue() {
        InMemoryJobQueue q = new InMemoryJobQueue();
        ScanSkuJob job = new ScanSkuJob(291003311L, 123L, 456L, "test");

        q.enqueue(job);

        BlockingQueue<QueueJob> backing = q.backingQueue();
        assertThat(backing).hasSize(1);
        assertThat(backing.peek()).isEqualTo(job);
    }

    @Test
    void backingQueue_returnsSameInstance() {
        InMemoryJobQueue q = new InMemoryJobQueue();
        BlockingQueue<QueueJob> a = q.backingQueue();
        BlockingQueue<QueueJob> b = q.backingQueue();

        assertThat(a).isSameAs(b);
    }
}
