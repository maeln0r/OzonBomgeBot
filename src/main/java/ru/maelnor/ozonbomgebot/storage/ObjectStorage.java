package ru.maelnor.ozonbomgebot.storage;

public interface ObjectStorage {
    /**
     * true, если объект существует
     */
    boolean exists(String bucket, String key) throws Exception;

    /**
     * положить объект (перезапишет, если есть)
     */
    void put(String bucket, String key, String contentType, byte[] data) throws Exception;

    /**
     * получить объект как байты
     */
    byte[] get(String bucket, String key) throws Exception;

    /**
     * убедиться, что бакет есть (создаст, если отсутствует)
     */
    void ensureBucket(String bucket) throws Exception;
}
