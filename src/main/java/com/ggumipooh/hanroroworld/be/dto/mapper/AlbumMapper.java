package com.ggumipooh.hanroroworld.be.dto.mapper;

import com.ggumipooh.hanroroworld.be.dto.AlbumDto;
import com.ggumipooh.hanroroworld.be.model.Album;

import java.util.List;
import java.util.Objects;

public final class AlbumMapper {
    private AlbumMapper() {
    }

    public static AlbumDto toDto(Album album) {
        if (album == null) {
            return null;
        }
        AlbumDto dto = new AlbumDto();
        dto.setId(album.getId());
        dto.setTitle(album.getTitle());
        dto.setPublishedAt(album.getPublishedAt());
        dto.setDescription(album.getDescription());
        dto.setMetadata(album.getMetadata());
        return dto;
    }

    public static List<AlbumDto> toDtoList(List<Album> albums) {
        return albums == null ? List.of()
                : albums.stream()
                        .filter(Objects::nonNull)
                        .map(AlbumMapper::toDto)
                        .toList();
    }
}
