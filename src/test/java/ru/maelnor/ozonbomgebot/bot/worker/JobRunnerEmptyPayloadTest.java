package ru.maelnor.ozonbomgebot.bot.worker;

import org.junit.jupiter.api.Test;
import ru.maelnor.ozonbomgebot.bot.entity.Job;
import ru.maelnor.ozonbomgebot.bot.flow.FlowIO;
import ru.maelnor.ozonbomgebot.bot.model.job.JobType;
import ru.maelnor.ozonbomgebot.bot.repository.PriceHistoryRepository;
import ru.maelnor.ozonbomgebot.bot.repository.TrackedItemRepository;
import ru.maelnor.ozonbomgebot.bot.service.JobQueueService;
import ru.maelnor.ozonbomgebot.bot.service.integration.ozon.OzonPriceService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JobRunnerEmptyPayloadTest {

    @Test
    void run_whenPayloadWithoutSku_currentImplementationThrowsNpe() throws Exception {
        JobQueueService jq = mock(JobQueueService.class);
        OzonPriceService ozon = mock(OzonPriceService.class);
        TrackedItemRepository trackedRepo = mock(TrackedItemRepository.class);
        PriceHistoryRepository priceRepo = mock(PriceHistoryRepository.class);
        FlowIO io = mock(FlowIO.class);

        JobRunner runner = new JobRunner(jq, ozon, trackedRepo, priceRepo, io);

        Job job = Job.builder()
                .id(10)
                .type(JobType.SCAN_SKU)
                .payloadJson("{}")
                .build();

        // вернули мапу без sku
        when(jq.readPayload(job)).thenReturn(Map.of());

        assertThatThrownBy(() -> runner.run(job))
                .isInstanceOf(NullPointerException.class);

        // и никого больше не трогаем
        verifyNoInteractions(ozon, trackedRepo, priceRepo, io);
    }
}
