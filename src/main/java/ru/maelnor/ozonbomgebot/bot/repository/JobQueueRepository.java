package ru.maelnor.ozonbomgebot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.maelnor.ozonbomgebot.bot.entity.Job;
import ru.maelnor.ozonbomgebot.bot.model.job.JobType;

import java.util.List;
import java.util.Optional;

public interface JobQueueRepository extends JpaRepository<Job, Integer> {

    @Modifying
    @Query(value = """
            insert into job(type, dedupe_key, payload_json, status, priority, run_at_ms, attempts, created_at_ms, updated_at_ms)
            values(:type, :dedupeKey, :payloadJson, 'PENDING', :priority, :runAt, 0, :now, :now)
            on conflict(type, dedupe_key) do update set
              priority = max(job.priority, excluded.priority),
              run_at_ms = min(job.run_at_ms, excluded.run_at_ms),
              updated_at_ms = excluded.updated_at_ms
            """, nativeQuery = true)
    int upsertPending(@Param("type") String type,
                      @Param("dedupeKey") String dedupeKey,
                      @Param("payloadJson") String payloadJson,
                      @Param("priority") int priority,
                      @Param("runAt") long runAt,
                      @Param("now") long now);

    /**
     * Атомарно вырезает одну задачу в RUNNING и отдает ее вызывающему.
     */
    @Modifying
    @Query(value = """
            with pick as (
              select id from job
              where status = 'PENDING' and run_at_ms <= :now
              order by priority desc, run_at_ms asc, attempts asc, id asc
              limit 1
            )
            update job
               set status = 'RUNNING', attempts = attempts + 1, updated_at_ms = :now
             where id in (select id from pick)
            returning *
            """, nativeQuery = true)
    List<Job> pickOneForRun(@Param("now") long now);

    @Modifying
    @Query(value = "update job set status='DONE', updated_at_ms=:now where id=:id", nativeQuery = true)
    int markDone(@Param("id") long id, @Param("now") long now);

    @Modifying
    @Query(value = """
            update job
               set status='PENDING', run_at_ms=:nextRunAt, last_error=:err, updated_at_ms=:now
             where id=:id
            """, nativeQuery = true)
    int reschedule(@Param("id") long id,
                   @Param("nextRunAt") long nextRunAt,
                   @Param("err") String err,
                   @Param("now") long now);

    @Modifying
    @Query(value = "update job set status='FAILED', last_error=:err, updated_at_ms=:now where id=:id", nativeQuery = true)
    int markFailed(@Param("id") long id, @Param("err") String err, @Param("now") long now);

    // Нужна верхняя граница очереди для SCAN_SKU, чтобы PriceWatcher знал от чего плясать
    @Query(value = "select max(j.run_at_ms) from job j where j.type = :type", nativeQuery = true)
    Optional<Long> findMaxRunAtByType(@Param("type") JobType type);
}