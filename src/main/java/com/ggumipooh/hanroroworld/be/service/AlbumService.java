package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.model.Album;
import com.ggumipooh.hanroroworld.be.Repository.AlbumRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;

    public List<Album> getAllAlbums() {
        return albumRepository.findAll();
    }
}
