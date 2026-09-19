package com.mediasequencer.repository;

import com.mediasequencer.entity.SyncEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SyncEventRepository extends JpaRepository<SyncEvent, Long> {

    List<SyncEvent> findByStartAtLessThanEqualOrderByStartAtDesc(Instant now, Pageable pageable);
}
