package ru.maelnor.ozonbomgebot.bot.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.maelnor.ozonbomgebot.bot.model.job.JobStatus;
import ru.maelnor.ozonbomgebot.bot.model.job.JobType;

@Entity
@Table(name = "job",
        uniqueConstraints = @UniqueConstraint(name = "ux_job_type_dedupe", columnNames = {"type", "dedupe_key"}),
        indexes = @Index(name = "ix_job_status_time", columnList = "status, run_at_ms"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Column(name = "dedupe_key")
    private String dedupeKey;

    @Column(name = "payload_json")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private Integer priority;

    @Column(name = "run_at_ms", nullable = false)
    private Long runAtMs;

    private Integer attempts;
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at_ms", nullable = false)
    private Long createdAtMs;
    @Column(name = "updated_at_ms", nullable = false)
    private Long updatedAtMs;
}