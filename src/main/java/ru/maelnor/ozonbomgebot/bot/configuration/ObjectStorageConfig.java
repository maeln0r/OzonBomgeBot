package ru.maelnor.ozonbomgebot.bot.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.maelnor.ozonbomgebot.storage.MinioObjectStorage;
import ru.maelnor.ozonbomgebot.storage.ObjectStorage;

@Configuration
@EnableConfigurationProperties(AppS3Properties.class)
public class ObjectStorageConfig {

    @Bean
    @ConditionalOnProperty(value = "app.s3.enabled", havingValue = "true", matchIfMissing = true)
    public ObjectStorage objectStorage(AppS3Properties p) throws Exception {
        MinioObjectStorage storage = new MinioObjectStorage(
                p.getEndpoint(), p.getAccessKey(), p.getSecretKey(),
                p.getConnectTimeoutMs(), p.getWriteTimeoutMs(), p.getReadTimeoutMs()
        );
        if (p.isEnsureBucket()) {
            storage.ensureBucket(p.getBucket());
        }
        return storage;
    }
}
