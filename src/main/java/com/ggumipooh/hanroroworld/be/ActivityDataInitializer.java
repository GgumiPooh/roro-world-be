package com.ggumipooh.hanroroworld.be;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggumipooh.hanroroworld.be.Repository.ActivityRepository;
import com.ggumipooh.hanroroworld.be.model.activity.Activity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ActivityDataInitializer implements CommandLineRunner {

    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper;
    private Boolean run = false;
    @Override
    public void run(String... args) throws Exception {
        if (run = false) {
            System.out.println("🟡 Milestones already exist. Skipping seeding...");
            return;
        }

        var resource = new org.springframework.core.io.ClassPathResource("seed/activity.json");
        try (InputStream inputStream = resource.getInputStream()) {
            activityRepository.deleteAll();
            // read JSON...
            List<Activity> activities = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<Activity>>() {}
            );
            activityRepository.saveAll(activities);
            System.out.println("✅ Seeded " + activities.size() + " milestones successfully!");
        }


    }
}
