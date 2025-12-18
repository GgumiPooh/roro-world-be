//package com.ggumipooh.hanroroworld.be;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.ggumipooh.hanroroworld.be.model.Album;
//import com.ggumipooh.hanroroworld.be.repository.AlbumRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.io.InputStream;
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class AlbumDataInitializer implements CommandLineRunner {
//
//    private final AlbumRepository albumRepository;
//    private final ObjectMapper objectMapper;
//    private Boolean run = false;
//
//    @Override
//    public void run(String... args) throws Exception {
//        if (run == false) {
//            System.out.println("🟡 Albums already exist. Skipping seeding...");
//            return;
//        }
//
//        var resource = new org.springframework.core.io.ClassPathResource("seed/album.json");
//        try (InputStream inputStream = resource.getInputStream()) {
//            albumRepository.deleteAll();
//            List<Album> albums = objectMapper.readValue(
//                    inputStream,
//                    new TypeReference<List<Album>>() {
//                    });
//            albumRepository.saveAll(albums);
//            System.out.println("✅ Seeded " + albums.size() + " albums successfully!");
//        }
//    }
//}
