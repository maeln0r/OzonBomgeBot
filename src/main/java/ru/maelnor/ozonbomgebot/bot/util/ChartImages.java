package ru.maelnor.ozonbomgebot.bot.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class ChartImages {
    private ChartImages() {
    }

    public static byte[] readAll(File f) throws IOException {
        return Files.readAllBytes(f.toPath());
    }

    public static File writeTemp(String prefix, byte[] bytes) throws IOException {
        File tmp = File.createTempFile(prefix, ".jpg");
        Files.write(tmp.toPath(), bytes);
        return tmp;
    }
}
