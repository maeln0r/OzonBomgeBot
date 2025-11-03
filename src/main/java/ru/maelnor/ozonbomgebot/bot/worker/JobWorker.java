package ru.maelnor.ozonbomgebot.bot.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.maelnor.ozonbomgebot.bot.entity.Job;
import ru.maelnor.ozonbomgebot.bot.service.JobQueueService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobWorker implements Runnable {
    private final JobQueueService jobQueueService;
    private final JobRunner jobRunner;

    private volatile boolean running = true;

    @Override
    public void run() {
        while (running) {
            try {
                var opt = jobQueueService.pickOne();
                if (opt.isEmpty()) {
                    Thread.sleep(750);
                    continue;
                }
                Job job = opt.get();
                try {
                    jobRunner.run(job);
                    jobQueueService.markDone(job.getId());
                } catch (Exception e) {
                    log.warn("Job {} {} failed: {}", job.getId(), job.getType(), e.toString());
                    jobQueueService.rescheduleWithBackoff(job, e.getMessage());
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker loop error", e);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public void shutdown() { running = false; }
}