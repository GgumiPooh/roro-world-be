package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.model.Song;
import com.ggumipooh.hanroroworld.be.repository.SongRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;

    public List<Song> getAll() {
        return songRepository.findAll();
    }

    public Song getById(Long id) {
        return songRepository.findById(id).orElse(null);
    }

    public List<Song> getSongsByAlbum(Long albumId) {
        return songRepository.findByAlbumId(albumId);
    }
}
