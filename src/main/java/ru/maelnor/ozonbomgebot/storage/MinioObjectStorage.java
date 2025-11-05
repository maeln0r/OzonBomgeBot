package ru.maelnor.ozonbomgebot.storage;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import okhttp3.OkHttpClient;

import java.io.ByteArrayInputStream;
import java.time.Duration;

public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient client;

    public MinioObjectStorage(String endpoint, String accessKey, String secretKey,
                              long connectTimeoutMs, long writeTimeoutMs, long readTimeoutMs) {
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .writeTimeout(Duration.ofMillis(writeTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .build();

        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .httpClient(http)
                .build();
    }

    @Override
    public boolean exists(String bucket, String key) throws Exception {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())
                    || "NoSuchObject".equals(e.errorResponse().code())) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void put(String bucket, String key, String contentType, byte[] data) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .contentType(contentType)
                            .stream(in, data.length, -1)
                            .build()
            );
        }
    }

    @Override
    public byte[] get(String bucket, String key) throws Exception {
        try (GetObjectResponse obj = client.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build())) {
            return obj.readAllBytes();
        }
    }

    @Override
    public void ensureBucket(String bucket) throws Exception {
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
