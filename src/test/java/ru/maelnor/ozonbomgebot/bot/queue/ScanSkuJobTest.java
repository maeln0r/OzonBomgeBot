package ru.maelnor.ozonbomgebot.bot.queue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScanSkuJobTest {

    @Test
    void recordStoresAllFields() {
        ScanSkuJob job = new ScanSkuJob(291003311L, 123L, 456L, "user request");

        assertThat(job.sku()).isEqualTo(291003311L);
        assertThat(job.chatId()).isEqualTo(123L);
        assertThat(job.userId()).isEqualTo(456L);
        assertThat(job.reason()).isEqualTo("user request");
    }
}
