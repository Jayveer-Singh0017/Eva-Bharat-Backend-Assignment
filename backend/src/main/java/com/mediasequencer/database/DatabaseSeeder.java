package com.mediasequencer.database;

import com.mediasequencer.entity.Media;
import com.mediasequencer.entity.Setting;
import com.mediasequencer.entity.Window;
import com.mediasequencer.entity.WindowItem;
import com.mediasequencer.repository.MediaRepository;
import com.mediasequencer.repository.SettingRepository;
import com.mediasequencer.repository.WindowItemRepository;
import com.mediasequencer.repository.WindowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    public static final Instant SEED_ANCHOR = Instant.parse("2026-01-01T00:00:00Z");
    public static final int DEFAULT_CYCLE_SECONDS = 18000; // 5 hours

    private final MediaRepository mediaRepository;
    private final WindowRepository windowRepository;
    private final WindowItemRepository windowItemRepository;
    private final SettingRepository settingRepository;

    public DatabaseSeeder(
            MediaRepository mediaRepository,
            WindowRepository windowRepository,
            WindowItemRepository windowItemRepository,
            SettingRepository settingRepository
    ) {
        this.mediaRepository = mediaRepository;
        this.windowRepository = windowRepository;
        this.windowItemRepository = windowItemRepository;
        this.settingRepository = settingRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (mediaRepository.count() > 0) {
            log.info("database: tables ready, seed data already present");
            return;
        }

        // Seed media rows
        List<Media> mediaList = List.of(
                new Media("M1", "image", "/media/m1.svg", 8),
                new Media("M2", "image", "/media/m2.svg", 10),
                new Media("M3", "image", "/media/m3.svg", 7),
                new Media("M4", "image", "/media/m4.svg", 9),
                new Media("M5", "video", "/media/m5.mp4", 15),
                new Media("M6", "blank", "", 5)
        );

        Map<String, Media> mediaByLabel = new LinkedHashMap<>();
        for (Media m : mediaList) {
            Media saved = mediaRepository.save(m);
            mediaByLabel.put(saved.getLabel(), saved);
        }

        // Seed windows and window items
        record SeedItem(String mediaLabel, int durationSeconds) {}
        record SeedWindow(String name, List<SeedItem> items) {}

        List<SeedWindow> seedWindows = List.of(
                new SeedWindow("Window 1", List.of(
                        new SeedItem("M1", 8),
                        new SeedItem("M2", 10),
                        new SeedItem("M3", 7)
                )),
                new SeedWindow("Window 2", List.of(
                        new SeedItem("M3", 7),
                        new SeedItem("M4", 9),
                        new SeedItem("M5", 15)
                )),
                new SeedWindow("Window 3", List.of(
                        new SeedItem("M5", 15),
                        new SeedItem("M1", 8),
                        new SeedItem("M6", 5),
                        new SeedItem("M2", 10)
                )),
                new SeedWindow("Window 4", List.of(
                        new SeedItem("M6", 5),
                        new SeedItem("M4", 9),
                        new SeedItem("M2", 10),
                        new SeedItem("M3", 7)
                ))
        );

        for (int i = 0; i < seedWindows.size(); i++) {
            SeedWindow sw = seedWindows.get(i);
            Window window = new Window(sw.name(), i + 1);
            window = windowRepository.save(window);

            for (int j = 0; j < sw.items().size(); j++) {
                SeedItem item = sw.items().get(j);
                Media media = mediaByLabel.get(item.mediaLabel());
                WindowItem wi = new WindowItem(window, media, j + 1, item.durationSeconds());
                windowItemRepository.save(wi);
            }
        }

        // Seed settings
        if (settingRepository.findByKey("cycle_seconds").isEmpty()) {
            settingRepository.save(new Setting("cycle_seconds", String.valueOf(DEFAULT_CYCLE_SECONDS)));
        }
        if (settingRepository.findByKey("anchor").isEmpty()) {
            settingRepository.save(new Setting("anchor", SEED_ANCHOR.toString()));
        }

        log.info("database: tables ready, seed data inserted");
    }
}
