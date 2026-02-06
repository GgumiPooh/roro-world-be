package com.ggumipooh.hanroroworld.be.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.ggumipooh.hanroroworld.be.dto.AlbumDto;
import com.ggumipooh.hanroroworld.be.dto.SongDto;
import com.ggumipooh.hanroroworld.be.dto.mapper.AlbumMapper;
import com.ggumipooh.hanroroworld.be.dto.mapper.SongMapper;
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
    public List<AlbumDto> getAlbums() {
        return AlbumMapper.toDtoList(albumService.getAllAlbums());
    }

    @GetMapping("/{albumId}")
    public List<SongDto> getSongs(@PathVariable Long albumId) {
        return songService.getSongsByAlbum(albumId);
    }

    @GetMapping("/song/{songId}")
    public SongDto getSong(@PathVariable Long songId) {
        return songService.getById(songId);
    }

}