package ru.maelnor.ozonbomgebot.bot.service;

import org.springframework.lang.Nullable;
import ru.maelnor.ozonbomgebot.bot.configuration.AppS3Properties;
import ru.maelnor.ozonbomgebot.storage.ObjectStorage;

public class ChartStorageService {
    private final @Nullable ObjectStorage storage;
    private final String bucket;

    public ChartStorageService(@Nullable ObjectStorage storage, AppS3Properties props) {
        this.storage = storage;
        this.bucket = props.getBucket();
    }

    public String key(long sku, long createdAtMs) {
        return "charts/%d/%d.jpg".formatted(sku, createdAtMs);
    }

    public boolean isEnabled() {
        return storage != null;
    }

    public boolean exists(long sku, long createdAtMs) throws Exception {
        if (storage == null) return false;
        return storage.exists(bucket, key(sku, createdAtMs));
    }

    public void upload(long sku, long createdAtMs, byte[] jpeg) throws Exception {
        if (storage == null) return;
        storage.put(bucket, key(sku, createdAtMs), "image/jpeg", jpeg);
    }

    public byte[] download(long sku, long createdAtMs) throws Exception {
        if (storage == null) throw new IllegalStateException("S3 disabled");
        return storage.get(bucket, key(sku, createdAtMs));
    }
}
