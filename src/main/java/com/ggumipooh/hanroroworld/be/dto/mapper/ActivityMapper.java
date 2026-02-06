package com.ggumipooh.hanroroworld.be.dto.mapper;

import com.ggumipooh.hanroroworld.be.dto.ActivityDto;
import com.ggumipooh.hanroroworld.be.model.activity.Activity;

public final class ActivityMapper {
    private ActivityMapper() {
    }

    public static ActivityDto toDto(Activity activity) {
        if (activity == null) {
            return null;
        }

        return ActivityDto.builder()
                .id(activity.getId())
                .title(activity.getTitle())
                .description(activity.getDescription())
                .activityType(activity.getType() != null ? activity.getType().name() : null)
                .metaData(activity.getMetaData())
                .activeFrom(activity.getActiveFrom())
                .activeTo(activity.getActiveTo())
                .build();
    }
}
