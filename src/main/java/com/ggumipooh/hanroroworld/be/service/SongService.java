package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.dto.SongDto;
import com.ggumipooh.hanroroworld.be.dto.mapper.SongMapper;
import com.ggumipooh.hanroroworld.be.repository.SongRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;

    public List<SongDto> getAll() {
        return songRepository.findAll().stream().map(SongMapper::toDto).toList();
    }

    public SongDto getById(Long id) {
        return songRepository.findById(id).map(SongMapper::toDto).orElse(null);
    }

    public List<SongDto> getSongsByAlbum(Long albumId) {
        return songRepository.findByAlbumId(albumId).stream().map(SongMapper::toDto).toList();
    }
}
