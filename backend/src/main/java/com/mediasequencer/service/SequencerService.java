package com.mediasequencer.service;

import com.mediasequencer.dto.*;
import com.mediasequencer.entity.*;
import com.mediasequencer.exception.BadRequestException;
import com.mediasequencer.exception.ResourceNotFoundException;
import com.mediasequencer.repository.*;
import com.mediasequencer.schedule.*;
import com.mediasequencer.sse.SseBroker;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SequencerService {

    public static final int MIN_CYCLE_SECONDS = 10;
    public static final int MAX_CYCLE_SECONDS = 86400;

    private static final Set<String> VALID_MEDIA_KINDS = Set.of("image", "video", "blank");

    private final MediaRepository mediaRepository;
    private final WindowRepository windowRepository;
    private final WindowItemRepository windowItemRepository;
    private final SyncEventRepository syncEventRepository;
    private final SettingRepository settingRepository;
    private final ScheduleService scheduleService;
    private final SseBroker sseBroker;

    public SequencerService(
            MediaRepository mediaRepository,
            WindowRepository windowRepository,
            WindowItemRepository windowItemRepository,
            SyncEventRepository syncEventRepository,
            SettingRepository settingRepository,
            ScheduleService scheduleService,
            SseBroker sseBroker
    ) {
        this.mediaRepository = mediaRepository;
        this.windowRepository = windowRepository;
        this.windowItemRepository = windowItemRepository;
        this.syncEventRepository = syncEventRepository;
        this.settingRepository = settingRepository;
        this.scheduleService = scheduleService;
        this.sseBroker = sseBroker;
    }

    @Transactional(readOnly = true)
    public StateResponseDto getState(Instant now) {
        Setting anchorSetting = settingRepository.findByKey("anchor")
                .orElseThrow(() -> new IllegalStateException("settings: missing \"anchor\""));
        Setting cycleSetting = settingRepository.findByKey("cycle_seconds")
                .orElseThrow(() -> new IllegalStateException("settings: missing \"cycle_seconds\""));

        Instant anchor = Instant.parse(anchorSetting.getValue());
        int cycleSeconds = Integer.parseInt(cycleSetting.getValue());

        List<Media> mediaList = mediaRepository.findAllByOrderByIdAsc();
        Map<Long, Media> mediaById = mediaList.stream()
                .collect(Collectors.toMap(Media::getId, m -> m));

        List<MediaDto> mediaDtos = mediaList.stream()
                .map(m -> new MediaDto(m.getId(), m.getLabel(), m.getKind(), m.getUrl(), m.getDefaultDurationSeconds()))
                .toList();

        List<Window> windows = windowRepository.findAllByOrderByPositionAsc();

        // Find active sync covering 'now'
        List<SyncEvent> recentSyncs = syncEventRepository.findByStartAtLessThanEqualOrderByStartAtDesc(
                now, PageRequest.of(0, 50)
        );

        SyncEvent activeSync = null;
        for (SyncEvent s : recentSyncs) {
            if (s.getStartAt().plusSeconds(s.getDurationSeconds()).isAfter(now)) {
                activeSync = s;
                break;
            }
        }

        ScheduleSyncEvent syncOverlay = null;
        SyncEventDto activeSyncDto = null;
        if (activeSync != null) {
            syncOverlay = new ScheduleSyncEvent(
                    activeSync.getMedia().getId(),
                    activeSync.getStartAt(),
                    activeSync.getDurationSeconds()
            );
            activeSyncDto = new SyncEventDto(
                    activeSync.getMedia().getId(),
                    activeSync.getStartAt().toString(),
                    activeSync.getDurationSeconds()
            );
        }

        List<WindowDto> windowDtos = new ArrayList<>(windows.size());
        for (Window win : windows) {
            List<WindowItem> items = win.getItems();
            List<WindowItemDto> itemDtos = new ArrayList<>(items.size());
            List<ScheduleItem> scheduleItems = new ArrayList<>(items.size());

            for (WindowItem it : items) {
                itemDtos.add(new WindowItemDto(
                        it.getId(),
                        it.getMedia().getId(),
                        it.getPosition(),
                        it.getDurationSeconds()
                ));
                scheduleItems.add(new ScheduleItem(it.getMedia().getId(), it.getDurationSeconds()));
            }

            ScheduleResolved resolved = scheduleService.resolve(anchor, scheduleItems, cycleSeconds, syncOverlay, now);

            ResolvedDto resolvedDto;
            double remainingSeconds = (double) resolved.remaining().toNanos() / 1_000_000_000.0;
            if (resolved.blank()) {
                resolvedDto = new ResolvedDto(
                        0L,
                        "blank",
                        "",
                        remainingSeconds,
                        resolved.isSync(),
                        true
                );
            } else {
                Media m = mediaById.get(resolved.mediaId());
                String kind = m != null ? m.getKind() : "blank";
                String url = m != null ? m.getUrl() : "";
                resolvedDto = new ResolvedDto(
                        resolved.mediaId(),
                        kind,
                        url,
                        remainingSeconds,
                        resolved.isSync(),
                        false
                );
            }

            windowDtos.add(new WindowDto(win.getId(), win.getName(), itemDtos, resolvedDto));
        }

        return new StateResponseDto(
                now.toString(),
                anchor.toString(),
                cycleSeconds,
                mediaDtos,
                windowDtos,
                activeSyncDto
        );
    }

    public WindowItemDto addWindowItem(Long windowId, AddItemRequest req) {
        if (req == null) {
            throw new BadRequestException("invalid request body");
        }
        if (req.mediaId() == null || req.mediaId() <= 0) {
            throw new BadRequestException("media_id is required");
        }
        if (req.durationSeconds() != null && req.durationSeconds() <= 0) {
            throw new BadRequestException("duration_seconds must be positive");
        }

        Window window = windowRepository.findById(windowId)
                .orElseThrow(() -> new ResourceNotFoundException("window " + windowId + ": not found"));

        Media media = mediaRepository.findById(req.mediaId())
                .orElseThrow(() -> new ResourceNotFoundException("media " + req.mediaId() + ": not found"));

        int duration = req.durationSeconds() != null ? req.durationSeconds() : media.getDefaultDurationSeconds();
        int position = windowItemRepository.findMaxPositionByWindowId(windowId) + 1;

        WindowItem item = new WindowItem(window, media, position, duration);
        item = windowItemRepository.save(item);

        sseBroker.broadcast("state_changed");

        return new WindowItemDto(item.getId(), media.getId(), item.getPosition(), item.getDurationSeconds());
    }

    public void deleteWindowItem(Long windowId, Long itemId) {
        if (!windowRepository.existsById(windowId)) {
            throw new ResourceNotFoundException("window " + windowId + ": not found");
        }

        int deleted = windowItemRepository.deleteByIdAndWindowId(itemId, windowId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("window item " + itemId + " in window " + windowId + ": not found");
        }

        sseBroker.broadcast("state_changed");
    }

    public MediaDto createMedia(CreateMediaRequest req) {
        if (req == null) {
            throw new BadRequestException("invalid request body");
        }
        if (req.label() == null || req.label().isBlank()) {
            throw new BadRequestException("label is required");
        }
        if (req.kind() == null || !VALID_MEDIA_KINDS.contains(req.kind())) {
            throw new BadRequestException("kind must be one of image, video, blank");
        }
        if ((req.url() == null || req.url().isBlank()) && !"blank".equals(req.kind())) {
            throw new BadRequestException("url is required unless kind is blank");
        }
        if (req.defaultDurationSeconds() == null || req.defaultDurationSeconds() <= 0) {
            throw new BadRequestException("default_duration_seconds must be positive");
        }

        String url = req.url() != null ? req.url() : "";
        Media media = new Media(req.label(), req.kind(), url, req.defaultDurationSeconds());
        media = mediaRepository.save(media);

        sseBroker.broadcast("state_changed");

        return new MediaDto(media.getId(), media.getLabel(), media.getKind(), media.getUrl(), media.getDefaultDurationSeconds());
    }

    public SyncEventDto createSync(CreateSyncRequest req) {
        if (req == null) {
            throw new BadRequestException("invalid request body");
        }
        if (req.mediaId() == null || req.mediaId() <= 0) {
            throw new BadRequestException("media_id is required");
        }
        if (req.durationSeconds() == null || req.durationSeconds() <= 0) {
            throw new BadRequestException("duration_seconds must be positive");
        }

        Media media = mediaRepository.findById(req.mediaId())
                .orElseThrow(() -> new ResourceNotFoundException("media " + req.mediaId() + ": not found"));

        Instant startAt = Instant.now();
        SyncEvent sync = new SyncEvent(media, startAt, req.durationSeconds());
        sync = syncEventRepository.save(sync);

        sseBroker.broadcast("sync");

        return new SyncEventDto(media.getId(), startAt.toString(), sync.getDurationSeconds());
    }

    public Map<String, Integer> setCycleSeconds(SetCycleRequest req) {
        if (req == null || req.cycleSeconds() == null || req.cycleSeconds() < MIN_CYCLE_SECONDS || req.cycleSeconds() > MAX_CYCLE_SECONDS) {
            throw new BadRequestException(
                    String.format("cycle_seconds must be between %d and %d", MIN_CYCLE_SECONDS, MAX_CYCLE_SECONDS)
            );
        }

        Setting setting = new Setting("cycle_seconds", String.valueOf(req.cycleSeconds()));
        settingRepository.save(setting);

        sseBroker.broadcast("state_changed");

        return Map.of("cycle_seconds", req.cycleSeconds());
    }
}
