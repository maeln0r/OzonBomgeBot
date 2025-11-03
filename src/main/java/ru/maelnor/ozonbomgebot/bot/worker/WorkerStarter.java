package ru.maelnor.ozonbomgebot.bot.worker;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class WorkerStarter implements ApplicationRunner {
    private final JobWorker worker;
    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    public void run(ApplicationArguments args) {
        executor.submit(worker);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}