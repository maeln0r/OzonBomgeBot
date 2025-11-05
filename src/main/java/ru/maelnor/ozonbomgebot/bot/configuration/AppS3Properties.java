package ru.maelnor.ozonbomgebot.bot.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
@Data
public class AppS3Properties {
    private boolean enabled;
    private String endpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private boolean ensureBucket;

    private long connectTimeoutMs = 5_000;
    private long writeTimeoutMs = 30_000;
    private long readTimeoutMs = 30_000;
}
