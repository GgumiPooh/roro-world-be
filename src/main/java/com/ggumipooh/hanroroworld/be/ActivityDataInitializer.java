package com.ggumipooh.hanroroworld.be;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggumipooh.hanroroworld.be.model.Album;
import com.ggumipooh.hanroroworld.be.model.Song;
import com.ggumipooh.hanroroworld.be.model.activity.Activity;
import com.ggumipooh.hanroroworld.be.repository.ActivityRepository;
import com.ggumipooh.hanroroworld.be.repository.AlbumRepository;
import com.ggumipooh.hanroroworld.be.repository.SongRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ActivityDataInitializer implements CommandLineRunner {

    private final ActivityRepository activityRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final ObjectMapper objectMapper;
    // Supabase에 데이터 넣을 때 true로 변경, 완료 후 다시 false로!
    private Boolean runActivity = false;
    private Boolean runAlbum = false;
    private Boolean runSong = false;

    @Override
    public void run(String... args) throws Exception {
        if (runActivity == false) {
            System.out.println("🟡 Milestones already exist. Skipping seeding...");
        } else if (runActivity == true) {

            var activityResource = new org.springframework.core.io.ClassPathResource("seed/activity.json");
            try (InputStream inputStream = activityResource.getInputStream()) {
                activityRepository.deleteAll();
                // read JSON...
                List<Activity> activities = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<Activity>>() {
                        });
                activityRepository.saveAll(activities);
                System.out.println("✅ Seeded " + activities.size() + " milestones successfully!");
            }
        }
        if (runAlbum == false) {
            System.out.println("🟡 Albums already exist. Skipping seeding...");
        } else if (runAlbum == true) {

            var albumResource = new org.springframework.core.io.ClassPathResource("seed/album.json");
            try (InputStream inputStream = albumResource.getInputStream()) {
                albumRepository.deleteAll();
                List<Album> albums = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<Album>>() {
                        });
                albumRepository.saveAll(albums);
                System.out.println("✅ Seeded " + albums.size() + " albums successfully!");
            }
        }
        if (runSong == true) {
            var songResource = new org.springframework.core.io.ClassPathResource("seed/song.json");
            try (InputStream inputStream = songResource.getInputStream()) {
                songRepository.deleteAll();
                List<Song> songs = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<Song>>() {
                        });
                songRepository.saveAll(songs);
                System.out.println("✅ Seeded " + songs.size() + " songs successfully!");
            }
        }
    }

}
