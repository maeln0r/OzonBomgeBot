package ru.maelnor.ozonbomgebot.bot.service;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.entity.Job;
import ru.maelnor.ozonbomgebot.bot.model.job.JobType;
import ru.maelnor.ozonbomgebot.bot.repository.JobQueueRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobQueueServiceTest {

    private final JobQueueRepository repo = mock(JobQueueRepository.class);
    private final JobQueueService service = new JobQueueService(repo);

    @Test
    void enqueue_writesJsonAndCallsRepo() {
        service.enqueue(JobType.SCAN_SKU, "sku:1", Map.of("sku", 1L), 10, 123L);

        verify(repo).upsertPending(eq(JobType.SCAN_SKU.name()),
                eq("sku:1"),
                eq("{\"sku\":1}"),
                eq(10),
                eq(123L),
                anyLong());
    }

    @Test
    void enqueueScanSku_buildsCorrectDedupeAndPayload() {
        // чтобы не зависеть от random в computeNextRunAtForScanSku, зовём перегрузку с runAt
        service.enqueueScanSku(291003311L, 999L);

        verify(repo).upsertPending(eq(JobType.SCAN_SKU.name()),
                eq("sku:291003311"),
                eq("{\"sku\":291003311}"),
                eq(10),
                eq(999L),
                anyLong());
    }

    @Test
    void enqueueNotifySkuDisabled_buildsCorrectJob() {
        service.enqueueNotifySkuDisabled(291003311L);

        verify(repo).upsertPending(eq(JobType.NOTIFY_SKU_DISABLED.name()),
                eq("sku:291003311"),
                eq("{\"sku\":291003311}"),
                eq(5),
                anyLong(),
                anyLong());
    }

    @Test
    void rescheduleWithBackoff_whenAttemptsLessThanMax_reschedules() {
        Job job = Job.builder()
                .id(1)
                .type(JobType.SCAN_SKU)
                .attempts(1) // первая неудача → backoff 15 сек
                .build();

        service.rescheduleWithBackoff(job, "oops");

        verify(repo, never()).markFailed(anyLong(), anyString(), anyLong());
        verify(repo).reschedule(eq(1L), anyLong(), eq("oops"), anyLong());
    }

    @Test
    void rescheduleWithBackoff_whenAttemptsReachedMax_marksFailed() {
        Job job = Job.builder()
                .id(1)
                .type(JobType.SCAN_SKU)
                .attempts(8) // == MAX_ATTEMPTS
                .build();

        service.rescheduleWithBackoff(job, "permanent error");

        verify(repo).markFailed(eq(1L), contains("permanent error"), anyLong());
        verify(repo, never()).reschedule(anyLong(), anyLong(), anyString(), anyLong());
    }

    @Test
    void readPayload_returnsEmptyOnNull() {
        Job job = Job.builder().payloadJson(null).build();

        assertThat(service.readPayload(job)).isEmpty();
    }

    @Test
    void readPayload_parsesJson() {
        Job job = Job.builder().payloadJson("{\"sku\":291003311}").build();

        Map<String, Object> map = service.readPayload(job);

        assertThat(map).containsEntry("sku", 291003311);
    }
}
