package ru.maelnor.ozonbomgebot.bot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.maelnor.ozonbomgebot.bot.service.ChartStorageService;
import ru.maelnor.ozonbomgebot.storage.ObjectStorage;

@Configuration
public class ChartStorageConfig {

    @Bean
    public ChartStorageService chartStorageService(
            AppS3Properties props,
            org.springframework.beans.factory.ObjectProvider<ObjectStorage> storageProvider
    ) {
        ObjectStorage storageOrNull = storageProvider.getIfAvailable();
        return new ChartStorageService(storageOrNull, props);
    }
}
