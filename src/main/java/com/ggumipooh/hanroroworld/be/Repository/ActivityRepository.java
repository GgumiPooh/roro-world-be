package com.ggumipooh.hanroroworld.be.repository;

import com.ggumipooh.hanroroworld.be.model.activity.Activity;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    Page<Activity> findByActiveFromBetween(LocalDate startInclusive, LocalDate endExclusive, Pageable pageable);

    Page<Activity> findAllBy(Pageable pageable);
}
