package ru.maelnor.ozonbomgebot.bot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maelnor.ozonbomgebot.bot.entity.Job;
import ru.maelnor.ozonbomgebot.bot.model.job.JobType;
import ru.maelnor.ozonbomgebot.bot.repository.JobQueueRepository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {
    private final JobQueueRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    private static final Duration BASE_BACKOFF = Duration.ofSeconds(15);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 8;

    @Transactional
    public void enqueue(JobType type, String dedupeKey, Object payload, int priority, long runAtMs) {
        long now = System.currentTimeMillis();
        String json;
        try {
            json = payload == null ? null : om.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Payload JSON error", e);
        }

        repo.upsertPending(type.name(), dedupeKey, json, priority, runAtMs, now);
    }

    @Transactional
    public void enqueueScanSku(long sku, long runAtMs) {
        Map<String, Object> p = Map.of("sku", sku);
        enqueue(JobType.SCAN_SKU, "sku:" + sku, p, 10, runAtMs);
    }

    // Перегрузка БЕЗ runAt - он считается на лету, чтобы не долбили через /remove+/add
    @Transactional
    public void enqueueScanSku(long sku) {
        long runAt = computeNextRunAtForScanSku();
        enqueueScanSku(sku, runAt);
    }

    @Transactional
    public void enqueueNotifySkuDisabled(long sku) {
        Map<String, Object> p = Map.of("sku", sku);
        enqueue(JobType.NOTIFY_SKU_DISABLED, "sku:" + sku, p, 5, System.currentTimeMillis());
    }

    // Считаем runAt на основе последнего job-а по этому типу
    @Transactional
    protected long computeNextRunAtForScanSku() {
        long now = System.currentTimeMillis();
        Optional<Long> last = findMaxRunAtForScanSku();
        long base = last.orElse(now);
        long jitter = (long) (Math.random() * 5_000L); // защита от пачки
        return base + jitter;
    }

    // Ходим в репу и достаем последнюю задачу SCAN_SKU
    @Transactional(readOnly = true)
    public Optional<Long> findMaxRunAtForScanSku() {
        return repo.findMaxRunAtByType(JobType.SCAN_SKU);
    }

    @Transactional
    public void enqueueScanNow(long sku) {
        enqueueScanSku(sku, System.currentTimeMillis());
    }

    @Transactional
    public Optional<Job> pickOne() {
        List<Job> rows = repo.pickOneForRun(System.currentTimeMillis());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    @Transactional
    public void markDone(long id) {
        repo.markDone(id, System.currentTimeMillis());
    }

    @Transactional
    public void rescheduleWithBackoff(Job job, String error) {
        long now = System.currentTimeMillis();
        int k = Math.max(1, job.getAttempts());
        long backoffMs = Math.min(MAX_BACKOFF.toMillis(), (long) (BASE_BACKOFF.toMillis() * Math.pow(2, k - 1)));
        long next = now + backoffMs;

        if (job.getAttempts() >= MAX_ATTEMPTS) {
            repo.markFailed(job.getId(), shorten(error), now);
            log.warn("Job {} {} failed permanently after {} attempts: {}", job.getId(), job.getType(), job.getAttempts(), error);
        } else {
            repo.reschedule(job.getId(), next, shorten(error), now);
        }
    }

    public Map<String, Object> readPayload(Job job) {
        if (job.getPayloadJson() == null || job.getPayloadJson().isBlank()) return Map.of();
        try {
            return om.readValue(job.getPayloadJson(), new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String shorten(String s) {
        if (s == null) return null;
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}