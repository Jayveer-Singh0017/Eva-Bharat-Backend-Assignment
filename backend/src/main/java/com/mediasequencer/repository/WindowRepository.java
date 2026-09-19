package com.mediasequencer.repository;

import com.mediasequencer.entity.Window;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WindowRepository extends JpaRepository<Window, Long> {
    List<Window> findAllByOrderByPositionAsc();
}
