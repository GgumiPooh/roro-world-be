package com.ggumipooh.hanroroworld.be.dto.mapper;

import com.ggumipooh.hanroroworld.be.dto.SongDto;
import com.ggumipooh.hanroroworld.be.model.Song;

import java.util.List;
import java.util.Objects;

public final class SongMapper {
    private SongMapper() {
    }

    public static SongDto toDto(Song song) {
        if (song == null) {
            return null;
        }
        SongDto dto = new SongDto();
        dto.setId(song.getId());
        dto.setTitle(song.getTitle());
        dto.setDescription(song.getDescription());
        dto.setLyrics(song.getLyrics());
        dto.setMetadata(song.getMetadata());
        dto.setAlbumId(song.getAlbum() != null ? song.getAlbum().getId() : null);
        return dto;
    }

    public static List<SongDto> toDtoList(List<Song> songs) {
        return songs == null ? List.of()
                : songs.stream()
                        .filter(Objects::nonNull)
                        .map(SongMapper::toDto)
                        .toList();
    }
}
