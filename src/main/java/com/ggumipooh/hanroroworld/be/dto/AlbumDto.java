package com.ggumipooh.hanroroworld.be.dto;

import com.ggumipooh.hanroroworld.be.model.LanguageData;
import com.ggumipooh.hanroroworld.be.model.Metadata;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AlbumDto {
    private Long id;
    private List<LanguageData> title;
    private LocalDate publishedAt;
    private List<LanguageData> description;
    private List<Metadata> metadata;
    private String albumType;
    private List<AlbumSongDto> songs;

    @Getter
    @Setter
    public static class AlbumSongDto {
        private Long id;
        private Integer trackNumber;
    }
}
