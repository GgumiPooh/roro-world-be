package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.dto.ActivityDto;
import com.ggumipooh.hanroroworld.be.dto.mapper.ActivityMapper;
import com.ggumipooh.hanroroworld.be.model.activity.Activity;
import com.ggumipooh.hanroroworld.be.repository.ActivityRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;

    @Transactional(readOnly = true)
    public Page<ActivityDto> getActivitiesWithOrder(Integer year, String order, int page, int size) {
        // Determine sort direction
        Sort.Direction direction = "oldest".equalsIgnoreCase(order) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "activeFrom"));

        Page<Activity> activities;
        if (year != null) {
            // Filter by year
            LocalDate startOfYear = LocalDate.of(year, 1, 1);
            LocalDate startOfNextYear = LocalDate.of(year + 1, 1, 1);
            activities = activityRepository.findByActiveFromBetween(startOfYear, startOfNextYear, pageable);
        } else {
            // All activities
            activities = activityRepository.findAllBy(pageable);
        }

        return activities.map(ActivityMapper::toDto);
    }
}
