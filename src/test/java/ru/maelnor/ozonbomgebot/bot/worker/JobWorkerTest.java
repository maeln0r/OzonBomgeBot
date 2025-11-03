package ru.maelnor.ozonbomgebot.bot.worker;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.entity.Job;
import ru.maelnor.ozonbomgebot.bot.model.job.JobStatus;
import ru.maelnor.ozonbomgebot.bot.model.job.JobType;
import ru.maelnor.ozonbomgebot.bot.service.JobQueueService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class JobWorkerTest {

    @Test
    void run_whenJobPicked_runsJobAndMarksDone() throws Exception {
        JobQueueService jq = mock(JobQueueService.class);
        JobRunner jr = mock(JobRunner.class);

        JobWorker worker = new JobWorker(jq, jr);

        Job job = Job.builder()
                .id(1)
                .type(JobType.SCAN_SKU)
                .status(JobStatus.PENDING)
                .payloadJson("{\"sku\":291003311}")
                .build();

        // первый вызов pickOne -> есть job, дальше пусть будет пусто
        when(jq.pickOne())
                .thenReturn(Optional.of(job))
                .thenReturn(Optional.empty());

        Thread t = new Thread(worker);
        t.start();

        // чуть подождём, чтобы поток успел выполнить первую итерацию
        Thread.sleep(100);

        // останавливаем воркера и прерываем поток, чтобы он не уснул на 750мс
        worker.shutdown();
        t.interrupt();
        t.join(500);

        // проверяем, что джоб отработали
        verify(jr).run(job);
        verify(jq).markDone(1);
        assertThat(t.isAlive()).isFalse();
    }

    @Test
    void run_whenJobRunnerThrows_reschedulesWithBackoff() throws Exception {
        JobQueueService jq = mock(JobQueueService.class);
        JobRunner jr = mock(JobRunner.class);

        JobWorker worker = new JobWorker(jq, jr);

        Job job = Job.builder()
                .id(2)
                .type(JobType.SCAN_SKU)
                .status(JobStatus.PENDING)
                .payloadJson("{\"sku\":1}")
                .build();

        when(jq.pickOne())
                .thenReturn(Optional.of(job))
                .thenReturn(Optional.empty());

        doThrow(new RuntimeException("boom"))
                .when(jr).run(job);

        Thread t = new Thread(worker);
        t.start();

        Thread.sleep(100);

        worker.shutdown();
        t.interrupt();
        t.join(500);

        verify(jq, never()).markDone(anyLong());
        verify(jq).rescheduleWithBackoff(job, "boom");
        assertThat(t.isAlive()).isFalse();
    }
}
