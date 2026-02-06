package com.ggumipooh.hanroroworld.be.controller;

import com.ggumipooh.hanroroworld.be.dto.ActivityDto;
import com.ggumipooh.hanroroworld.be.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/activity")
public class ActivityController {
    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<Page<ActivityDto>> getActivities(
            @RequestParam(name = "year", required = false) String yearParam,
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "4") int size) {
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
        return ResponseEntity.ok(activityService.getActivitiesWithOrder(year, sort, page, size));
    }
}
