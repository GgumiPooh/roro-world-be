package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.model.activity.Activity;
import com.ggumipooh.hanroroworld.be.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/activity")
public class ActivityController {
    private final ActivityService activityService;

    @GetMapping
    public List<Activity> getActivities(
            @RequestParam(name = "year", required = false) String yearParam,
            @RequestParam(name = "sort", defaultValue = "latest") String sort) {
        Integer year = null;
        if (yearParam != null) {
            String trimmed = yearParam.trim();
            if (!trimmed.isEmpty()) {
                try {
                    year = Integer.parseInt(trimmed);
                } catch (NumberFormatException ignored) {
                    year = null;
                }
            }
        }
        return activityService.getActivitiesWithOrder(year, sort);
    }

}
