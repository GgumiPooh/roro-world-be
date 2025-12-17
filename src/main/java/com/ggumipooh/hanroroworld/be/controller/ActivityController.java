package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.model.activity.Activity;
import com.ggumipooh.hanroroworld.be.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activity")
public class ActivityController {
    private final ActivityService activityService;

    @GetMapping("/{year}")
    public List<Activity> getActivities(@RequestParam(required = false) Integer year) {
        if (year == null) {
            return activityService.getAllActivities();
        }
        return activityService.getActivitiesStartingInYear(year);
    }

   
}
