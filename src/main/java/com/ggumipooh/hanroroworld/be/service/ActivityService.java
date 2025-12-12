package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.model.activity.Activity;
import com.ggumipooh.hanroroworld.be.Repository.ActivityRepository;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;

    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    public List<Activity> getActivitiesStartingInYear(int year) {
        var start = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        var end = start.plus(1, ChronoUnit.YEARS);
        return activityRepository.findAllByActiveFromGreaterThanEqualAndActiveFromLessThan(start, end);
    }
}
