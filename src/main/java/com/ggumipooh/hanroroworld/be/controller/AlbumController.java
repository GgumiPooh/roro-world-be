package com.ggumipooh.hanroroworld.be.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.ggumipooh.hanroroworld.be.model.Album;
import com.ggumipooh.hanroroworld.be.model.Song;
import com.ggumipooh.hanroroworld.be.service.AlbumService;
import com.ggumipooh.hanroroworld.be.service.SongService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/album")
public class AlbumController {
    private final AlbumService albumService;
    private final SongService songService;

    @GetMapping
    public List<Album> getAlbums() {
        return albumService.getAllAlbums();
    }

    @GetMapping("/{albumId}")
    public List<Song> getSongs(@PathVariable Long albumId) {
        return songService.getSongsByAlbum(albumId);
    }

    @GetMapping("/songs/{songId}")
    public Song getSong(@PathVariable Long songId) {
        return songService.getById(songId);
    }

}