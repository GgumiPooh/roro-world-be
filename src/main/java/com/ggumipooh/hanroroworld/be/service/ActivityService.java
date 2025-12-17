package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.model.activity.Activity;
import com.ggumipooh.hanroroworld.be.repository.ActivityRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Comparator;
import java.time.ZonedDateTime;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ActivityService {
	private final ActivityRepository activityRepository;

	public List<Activity> getAllActivities() {
		return activityRepository.findAll();
	}

	public List<Activity> getActivitiesStartingInYear(int year) {
		ZonedDateTime startZdt = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.of("Asia/Seoul"));
		Instant start = startZdt.toInstant();
		Instant end = startZdt.plusYears(1).toInstant();
		return activityRepository.findAllByActiveFromGreaterThanEqualAndActiveFromLessThan(start, end);
	}

	public List<Activity> getActivitiesWithOrder(Integer year, String order) {
		List<Activity> activities = (year == null)
				? getAllActivities()
				: getActivitiesStartingInYear(year);

		Comparator<Activity> byActiveFromAsc = Comparator.comparing(
				Activity::getActiveFrom,
				java.util.Comparator.nullsLast(Comparator.naturalOrder()));
		boolean isOldest = "oldest".equalsIgnoreCase(order);
		activities.sort(isOldest ? byActiveFromAsc : byActiveFromAsc.reversed());
		return activities;
	}
}
